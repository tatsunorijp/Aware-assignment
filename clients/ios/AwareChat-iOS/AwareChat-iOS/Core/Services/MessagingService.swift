import Foundation

/// App-owned synchronization. Only its event loop advances the connection and FIFO outbox.
actor MessagingService: MessagingServiceProtocol {
  private let socket: any WebSocketClientProtocol
  private let users: any UserLocalRepository
  private let messages: any MessageLocalRepository
  private let clock: any MessagingClock

  private var state = MessagingConnectionState.disconnected
  private var observers: [UUID: AsyncStream<MessagingConnectionState>.Continuation] = [:]
  private var issueObservers: [UUID: AsyncStream<MessagingIssue>.Continuation] = [:]
  private var worker: Task<Void, Never>?
  private var runID: UUID?
  private var inbox: AsyncStream<MessagingSignal>.Continuation?
  private var sessionID: UUID?
  private var identity: UserDTO?
  private var reader: Task<Void, Never>?
  private var writes: [UUID: Task<Void, Never>] = [:]
  private var deadline: Task<Void, Never>?
  private var deadlineID: UUID?
  private var retryTask: Task<Void, Never>?
  private var retryID: UUID?
  private var retryPolicy = MessagingRetryPolicy()
  private var inFlight: UUID?
  private var retryMessageID: UUID?
  private var attemptedMessages: Set<UUID> = []

  init(
    socket: any WebSocketClientProtocol,
    users: any UserLocalRepository,
    messages: any MessageLocalRepository,
    clock: any MessagingClock = SystemMessagingClock()
  ) {
    self.socket = socket
    self.users = users
    self.messages = messages
    self.clock = clock
  }

  /// Save/reuse the identity through UserLocalRepository before starting.
  /// Repeated starts are idempotent. Await stop() before an explicit restart.
  func start() {
    guard worker == nil else { return }
    let id = UUID()
    let (stream, continuation) = AsyncStream<MessagingSignal>.makeStream()
    runID = id
    inbox = continuation
    retryPolicy.reset()
    retryMessageID = nil
    attemptedMessages.removeAll()
    setState(.connecting)
    worker = Task { [weak self] in await self?.run(stream: stream, id: id) }
    continuation.yield(.connect)
  }

  func stop() async {
    let id = runID
    let activeWorker = worker
    activeWorker?.cancel()
    inbox?.finish()
    await activeWorker?.value
    // A newer explicit start must not be erased by this stop's suspended continuation.
    if runID == id || runID == nil { setState(.disconnected) }
  }

  @discardableResult
  func sendMessage(text: String, receiverId: UUID) async throws -> LocalMessage {
    let message = try await messages.enqueue(text: text, receiverId: receiverId, createdAt: clock.now())
    inbox?.yield(.wakeOutbox)
    return message
  }

  func connectionState() -> MessagingConnectionState { state }

  func observeConnectionState() -> AsyncStream<MessagingConnectionState> {
    let id = UUID()
    let (stream, continuation) = AsyncStream<MessagingConnectionState>.makeStream(
      bufferingPolicy: .bufferingNewest(1)
    )
    observers[id] = continuation
    continuation.yield(state)
    continuation.onTermination = { [weak self] _ in
      Task { await self?.removeObserver(id) }
    }
    return stream
  }

  /// Subscribe before requesting work. Issues are transient, not another copy of message history.
  func observeIssues() -> AsyncStream<MessagingIssue> {
    let id = UUID()
    let (stream, continuation) = AsyncStream<MessagingIssue>.makeStream(bufferingPolicy: .bufferingNewest(1))
    issueObservers[id] = continuation
    continuation.onTermination = { [weak self] _ in
      Task { await self?.removeIssueObserver(id) }
    }
    return stream
  }

  private func run(stream: AsyncStream<MessagingSignal>, id: UUID) async {
    do {
      try await messages.recoverInterruptedSends()
      for await signal in stream {
        try Task.checkCancellation()
        try await handle(signal)
      }
    } catch {
      if !Task.isCancelled { setState(.connectionFailure(failure(from: error))) }
    }
    await closeSession()
    do { try await messages.recoverInterruptedSends() }
    catch { setState(.connectionFailure(failure(from: error))) }
    if runID == id {
      inbox?.finish()
      inbox = nil
      worker = nil
      runID = nil
    }
  }

  private func handle(_ signal: MessagingSignal) async throws {
    switch signal {
    case .connect:
      try await connect()
    case .wakeOutbox:
      try await flushNext()
    case .event(let session, let event):
      guard sessionID == session else { return }
      try await handle(event)
    case .lost(let session, let error):
      guard sessionID == session else { return }
      if case .webSocketClosed(code: 4001, reason: _) = error {
        throw MessagingFailure.sessionReplaced
      }
      try await reconnect(after: .network(error))
    case .writeFinished(let session, let writeID, let error):
      guard sessionID == session else { return }
      writes.removeValue(forKey: writeID)
      if let error { try await reconnect(after: .network(error)) }
    case .deadline(let session, let token):
      guard sessionID == session, deadlineID == token else { return }
      try await reconnect(after: identity == nil ? .identificationTimedOut : .acceptanceTimedOut)
    case .retry(let token):
      guard retryID == token else { return }
      retryTask = nil
      retryID = nil
      if sessionID == nil { try await connect() }
      else { try await flushNext() }
    }
  }

  private func connect() async throws {
    guard let current = try await users.currentUser() else { throw PersistenceError.missingIdentity }
    let user = try current.wireIdentity()
    try Task.checkCancellation()
    setState(.connecting)
    let session = UUID()
    sessionID = session
    let events = await socket.connect()
    try Task.checkCancellation()
    guard let inbox else { return }
    reader = Task {
      do {
        for try await event in events {
          try Task.checkCancellation()
          inbox.yield(.event(session: session, event))
        }
        if !Task.isCancelled { inbox.yield(.lost(session: session, .notConnected)) }
      } catch {
        if !Task.isCancelled { inbox.yield(.lost(session: session, NetworkError.wrapping(error))) }
      }
    }
    setDeadline(MessagingRetryPolicy.identificationTimeout)
    write(.identify(user: user))
  }

  private func handle(_ event: ServerEvent) async throws {
    switch event {
    case .identityAccepted(let accepted):
      guard identity == nil else { return }
      guard let current = try await users.currentUser(), try current.wireIdentity() == accepted else {
        throw MessagingFailure.unexpectedIdentity
      }
      try Task.checkCancellation()
      try await users.completeRegistration(acceptedUser: accepted)
      try Task.checkCancellation()
      identity = accepted
      cancelDeadline()
      if retryMessageID == nil { retryPolicy.reset() }
      setState(.connected)
      try await flushNext()
    case .syncCompleted:
      // Replay completion is not a persistence ACK or an identification gate.
      break
    case .incomingMessage(let message):
      guard identity != nil else { throw MessagingFailure.network(.invalidWebSocketFrame) }
      try await messages.persistIncoming(message, receivedAt: clock.now())
      try Task.checkCancellation()
      write(.messagePersisted(messageId: message.messageId))
    case .messageAccepted(let id, let timestamp):
      guard identity != nil, attemptedMessages.contains(id),
            let message = try await messages.message(id: id), message.direction == .outgoing else { return }
      try await messages.markAccepted(id: id, serverReceivedAt: timestamp)
      try Task.checkCancellation()
      attemptedMessages.remove(id)
      if inFlight == id || retryMessageID == id {
        inFlight = nil
        retryMessageID = nil
        cancelDeadline()
        cancelRetry()
        retryPolicy.reset()
        try await flushNext()
      }
    case .protocolError(let event):
      try await handleProtocolError(event)
    }
  }

  private func handleProtocolError(_ event: ProtocolErrorEvent) async throws {
    let error = event.error
    let issue = MessagingIssue(messageId: event.messageId.flatMap(UUID.init(uuidString:)), error: error)
    for observer in issueObservers.values { observer.yield(issue) }
    if error.code == "SESSION_REPLACED" { throw MessagingFailure.sessionReplaced }
    // These errors describe recipient acknowledgements, not outgoing message acceptance.
    if error.code == "UNKNOWN_MESSAGE" || error.code == "NOT_RECEIVER" { return }
    if let rawID = event.messageId, let id = UUID(uuidString: rawID) {
      guard id == inFlight || id == retryMessageID,
            let message = try await messages.message(id: id),
            message.direction == .outgoing,
            message.state == .sending || message.state == .pendingToSend else { return }
      try await messages.markRejected(id: id, retryable: error.isRetryable)
      try Task.checkCancellation()
      inFlight = nil
      cancelDeadline()
      if error.isRetryable {
        retryMessageID = id
        scheduleRetry()
      } else {
        attemptedMessages.remove(id)
        retryMessageID = nil
        cancelRetry()
        retryPolicy.reset()
        try await flushNext()
      }
    } else if event.messageId == nil {
      if error.isRetryable { try await reconnect(after: .server(error)) }
      else { throw MessagingFailure.server(error) }
    }
  }

  private func flushNext() async throws {
    guard identity != nil, inFlight == nil, retryID == nil else { return }
    guard let message = try await messages.pendingMessages().first else { return }
    try Task.checkCancellation()
    try await messages.markSending(id: message.id)
    try Task.checkCancellation()
    inFlight = message.id
    attemptedMessages.insert(message.id)
    setDeadline(MessagingRetryPolicy.acceptanceTimeout)
    write(.sendMessage(message: try message.outgoingPayload()))
  }

  private func reconnect(after failure: MessagingFailure) async throws {
    if let inFlight { retryMessageID = inFlight }
    await closeSession()
    try Task.checkCancellation()
    try await messages.recoverInterruptedSends()
    try Task.checkCancellation()
    setState(.connectionFailure(failure))
    scheduleRetry()
  }

  private func write(_ event: ClientEvent) {
    guard let session = sessionID, let inbox else { return }
    let id = UUID()
    let socket = socket
    writes[id] = Task {
      do {
        try Task.checkCancellation()
        try await socket.send(event)
        if !Task.isCancelled { inbox.yield(.writeFinished(session: session, id, nil)) }
      } catch {
        if !Task.isCancelled {
          inbox.yield(.writeFinished(session: session, id, NetworkError.wrapping(error)))
        }
      }
    }
  }

  private func setDeadline(_ duration: Duration) {
    cancelDeadline()
    guard let session = sessionID, let inbox else { return }
    let token = UUID()
    deadlineID = token
    let clock = clock
    deadline = Task {
      do {
        try await clock.sleep(for: duration)
        try Task.checkCancellation()
        inbox.yield(.deadline(session: session, token))
      } catch { /* Cancellation invalidates this timer. */ }
    }
  }

  private func scheduleRetry() {
    cancelRetry()
    guard let inbox else { return }
    let token = UUID()
    retryID = token
    let delay = retryPolicy.nextDelay()
    let clock = clock
    retryTask = Task {
      do {
        try await clock.sleep(for: delay)
        try Task.checkCancellation()
        inbox.yield(.retry(token))
      } catch { /* Cancellation invalidates this timer. */ }
    }
  }

  private func closeSession() async {
    sessionID = nil
    identity = nil
    inFlight = nil
    cancelDeadline()
    cancelRetry()
    reader?.cancel()
    reader = nil
    for task in writes.values { task.cancel() }
    writes.removeAll()
    await socket.disconnect()
  }

  private func cancelDeadline() {
    deadline?.cancel()
    deadline = nil
    deadlineID = nil
  }

  private func cancelRetry() {
    retryTask?.cancel()
    retryTask = nil
    retryID = nil
  }

  private func setState(_ newState: MessagingConnectionState) {
    guard state != newState else { return }
    state = newState
    for observer in observers.values { observer.yield(newState) }
  }

  private func removeObserver(_ id: UUID) { observers.removeValue(forKey: id) }
  private func removeIssueObserver(_ id: UUID) { issueObservers.removeValue(forKey: id) }

  private func failure(from error: any Error) -> MessagingFailure {
    if let error = error as? MessagingFailure { return error }
    if let error = error as? PersistenceError { return .storage(error) }
    return .network(NetworkError.wrapping(error))
  }
}

private nonisolated enum MessagingSignal: Sendable {
  case connect
  case wakeOutbox
  case event(session: UUID, ServerEvent)
  case lost(session: UUID, NetworkError)
  case writeFinished(session: UUID, UUID, NetworkError?)
  case deadline(session: UUID, UUID)
  case retry(UUID)
}
