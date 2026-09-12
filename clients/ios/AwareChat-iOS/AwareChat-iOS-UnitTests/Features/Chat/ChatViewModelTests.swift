// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Chat ViewModel")
struct ChatViewModelTests {
  @Test
  func loadResolvesPeerAndPublishesLocalHistory() async throws {
    let initial = try makeLocalMessage(text: "Hello", state: .sent)
    let harness = ChatHarness(messages: [initial])

    await harness.viewModel.load()

    try await eventually {
      harness.viewModel.screenState == .ready([initial])
    }
    #expect(harness.viewModel.peerName == "Bob")
    #expect(harness.conversations.requestedPeerIds == [ChatIDs.peer])
    #expect(harness.messages.observedConversationIds == [ChatIDs.conversation])
  }

  @Test
  func emptyHistoryIsReadyAndCanCompose() async throws {
    let harness = ChatHarness()

    await harness.viewModel.load()

    try await eventually { harness.viewModel.screenState == .ready([]) }
    harness.viewModel.draft = "First message"
    #expect(harness.viewModel.canSend)
  }

  @Test
  func committedHistoryUpdatesRefreshAckStateWithoutReloading() async throws {
    let pending = try makeLocalMessage(text: "Queued", state: .pendingToSend)
    let sent = try makeLocalMessage(text: "Queued", state: .sent)
    let harness = ChatHarness(messages: [pending])
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([pending]) }

    harness.messages.emit([sent])

    try await eventually { harness.viewModel.screenState == .ready([sent]) }
    #expect(harness.messages.observedConversationIds.count == 1)
  }

  @Test
  func localHistoryFailureCanBeRetried() async throws {
    let harness = ChatHarness()
    harness.messages.observationError = .readFailed
    await harness.viewModel.load()
    try await eventually {
      harness.viewModel.screenState == .error(PersistenceError.readFailed.localizedDescription)
    }

    harness.messages.observationError = nil
    harness.viewModel.retryLocalLoad()

    try await eventually { harness.viewModel.screenState == .ready([]) }
    #expect(harness.conversations.requestedPeerIds == [ChatIDs.peer, ChatIDs.peer])
  }

  @Test
  func blankDraftIsRejectedWithoutCallingMessagingService() async throws {
    let harness = ChatHarness()
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([]) }
    harness.viewModel.draft = "  \n "

    await harness.viewModel.send()

    #expect(harness.viewModel.validationMessage == "Enter a message.")
    #expect(await harness.messaging.requests.isEmpty)
  }

  @Test
  func successfulSendUsesPeerIdAndClearsUnchangedDraft() async throws {
    let queued = try makeLocalMessage(text: "Offline hello", state: .pendingToSend)
    let harness = ChatHarness(sendResponse: .success(queued))
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([]) }
    harness.viewModel.draft = "Offline hello"

    await harness.viewModel.send()

    #expect(
      await harness.messaging.requests == [
        ChatSendRequest(text: "Offline hello", receiverId: ChatIDs.peer)
      ]
    )
    #expect(harness.viewModel.draft.isEmpty)
    #expect(harness.viewModel.sendErrorMessage == nil)
  }

  @Test
  func failedLocalSendKeepsDraftAndShowsContextualError() async throws {
    let harness = ChatHarness(sendResponse: .failure(.storage(.writeFailed)))
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([]) }
    harness.viewModel.draft = "Keep this text"

    await harness.viewModel.send()

    #expect(harness.viewModel.draft == "Keep this text")
    #expect(harness.viewModel.sendErrorMessage == PersistenceError.writeFailed.localizedDescription)
    #expect(harness.viewModel.screenState == .ready([]))
  }

  @Test
  func duplicateSendAttemptIsIgnoredWhileOneIsInFlight() async throws {
    let harness = ChatHarness(sendResponse: .suspended)
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([]) }
    harness.viewModel.draft = "Only once"

    let firstSend = Task { await harness.viewModel.send() }
    defer { firstSend.cancel() }
    try await eventually { await harness.messaging.requests.count == 1 }

    await harness.viewModel.send()

    #expect(await harness.messaging.requests.count == 1)
  }

  @Test
  func connectionFailureAndRetryDoNotHideHistory() async throws {
    let message = try makeLocalMessage(text: "Still visible", state: .sent)
    let harness = ChatHarness(messages: [message])
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([message]) }

    await harness.messaging.emit(.connectionFailure(.network(.notConnected)))

    try await eventually {
      harness.viewModel.connectionState == .offline(
        MessagingFailure.network(.notConnected).localizedDescription
      )
    }
    #expect(harness.viewModel.screenState == .ready([message]))

    harness.viewModel.retryConnection()
    try await eventually {
      let stopCount = await harness.messaging.stopCount
      let startCount = await harness.messaging.startCount
      return stopCount == 1 && startCount == 1
    }
    #expect(harness.viewModel.screenState == .ready([message]))
  }

  @Test
  func structuredServerIssueStaysWithMessageUntilAcceptance() async throws {
    let pending = try makeLocalMessage(text: "Retrying", state: .pendingToSend)
    let sent = try makeLocalMessage(text: "Retrying", state: .sent)
    let harness = ChatHarness(messages: [pending])
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([pending]) }
    let issue = try ServerError(
      code: "TEMPORARY_UNAVAILABLE",
      userMessage: "Please try again later.",
      developerMessage: "Internal detail",
      isRetryable: true,
      requestId: nil
    )
    try await eventually { await harness.messaging.issueObserverCount == 1 }

    await harness.messaging.emitIssue(MessagingIssue(messageId: ChatIDs.message, error: issue))

    try await eventually {
      harness.viewModel.latestIssue == ChatIssuePresentation(
        messageId: ChatIDs.message,
        message: "Please try again later."
      )
    }
    harness.messages.emit([sent])
    try await eventually { harness.viewModel.latestIssue == nil }
  }

  @Test
  func uncorrelatedIssueCanBeDismissedWithoutHidingHistory() async throws {
    let message = try makeLocalMessage(text: "Still visible", state: .sent)
    let harness = ChatHarness(messages: [message])
    await harness.viewModel.load()
    try await eventually { harness.viewModel.screenState == .ready([message]) }
    let issue = try ServerError(
      code: "TEMPORARY_UNAVAILABLE",
      userMessage: "Service is temporarily unavailable.",
      developerMessage: nil,
      isRetryable: true,
      requestId: nil
    )
    try await eventually { await harness.messaging.issueObserverCount == 1 }

    await harness.messaging.emitIssue(MessagingIssue(messageId: nil, error: issue))

    try await eventually {
      harness.viewModel.latestIssue?.message == "Service is temporarily unavailable."
    }
    harness.viewModel.dismissLatestIssue()
    #expect(harness.viewModel.latestIssue == nil)
    #expect(harness.viewModel.screenState == .ready([message]))
  }

  @Test
  func missingCachedNameUsesNeutralFallback() async throws {
    let harness = ChatHarness(peerName: nil)

    await harness.viewModel.load()

    try await eventually { harness.viewModel.screenState == .ready([]) }
    #expect(harness.viewModel.peerName == "Unknown user")
  }
}

@MainActor
private final class ChatHarness {
  let conversations: ChatFakeConversationRepository
  let messages: ChatFakeMessageRepository
  let messaging: ChatFakeMessagingService
  lazy var viewModel = ChatViewModel(
    peerId: ChatIDs.peer,
    conversations: conversations,
    messages: messages,
    messaging: messaging
  )

  init(
    peerName: String? = "Bob",
    messages: [LocalMessage] = [],
    sendResponse: ChatFakeMessagingService.SendResponse = .suspended
  ) {
    conversations = ChatFakeConversationRepository(peerName: peerName)
    self.messages = ChatFakeMessageRepository(messages: messages)
    messaging = ChatFakeMessagingService(sendResponse: sendResponse)
  }
}

@MainActor
private final class ChatFakeConversationRepository: ConversationLocalRepository {
  var error: PersistenceError?
  private(set) var requestedPeerIds = [UUID]()
  private let peerName: String?

  init(peerName: String?) {
    self.peerName = peerName
  }

  func conversations() throws -> [LocalConversation] { [] }

  func conversation(id: String) throws -> LocalConversation? { nil }

  func getOrCreate(with userId: UUID) throws -> LocalConversation {
    requestedPeerIds.append(userId)
    if let error { throw error }
    return LocalConversation(
      conversationId: ChatIDs.conversation,
      peer: LocalUser(
        userId: userId,
        name: peerName,
        isCurrent: false,
        registrationCompleted: false
      ),
      latestMessage: nil
    )
  }

  func observeConversations() -> AsyncThrowingStream<[LocalConversation], any Error> {
    AsyncThrowingStream { $0.finish() }
  }
}

@MainActor
private final class ChatFakeMessageRepository: MessageLocalRepository {
  var observationError: PersistenceError?
  private(set) var observedConversationIds = [String]()
  private var snapshot: [LocalMessage]
  private var observers = [AsyncThrowingStream<[LocalMessage], any Error>.Continuation]()

  init(messages: [LocalMessage]) {
    snapshot = messages
  }

  func message(id: UUID) throws -> LocalMessage? {
    snapshot.first { $0.id == id }
  }

  func messages(conversationId: String) throws -> [LocalMessage] { snapshot }

  func pendingMessages() throws -> [LocalMessage] {
    snapshot.filter { $0.direction == .outgoing && $0.state == .pendingToSend }
  }

  func enqueue(text: String, receiverId: UUID, createdAt: Date) throws -> LocalMessage {
    throw PersistenceError.writeFailed
  }

  func persistIncoming(_ message: MessageDTO, receivedAt: Date) throws -> LocalMessage {
    throw PersistenceError.writeFailed
  }

  func markSending(id: UUID) throws { }

  func markAccepted(id: UUID, serverReceivedAt: Date) throws { }

  func markRejected(id: UUID, retryable: Bool) throws { }

  func recoverInterruptedSends() throws { }

  func observeMessages(
    conversationId: String
  ) -> AsyncThrowingStream<[LocalMessage], any Error> {
    observedConversationIds.append(conversationId)
    return AsyncThrowingStream { continuation in
      if let observationError {
        continuation.finish(throwing: observationError)
      } else {
        observers.append(continuation)
        continuation.yield(snapshot)
      }
    }
  }

  func emit(_ messages: [LocalMessage]) {
    snapshot = messages
    for observer in observers { observer.yield(messages) }
  }
}

nonisolated private struct ChatSendRequest: Equatable, Sendable {
  let text: String
  let receiverId: UUID
}

private actor ChatFakeMessagingService: MessagingServiceProtocol {
  nonisolated enum SendResponse: Sendable {
    case success(LocalMessage)
    case failure(MessagingFailure)
    case suspended
  }

  private(set) var requests = [ChatSendRequest]()
  private(set) var startCount = 0
  private(set) var stopCount = 0
  private var state = MessagingConnectionState.connected
  private var observers = [AsyncStream<MessagingConnectionState>.Continuation]()
  private var issueObservers = [AsyncStream<MessagingIssue>.Continuation]()
  private let sendResponse: SendResponse

  var issueObserverCount: Int { issueObservers.count }

  init(sendResponse: SendResponse) {
    self.sendResponse = sendResponse
  }

  func start() {
    startCount += 1
    setState(.connecting)
  }

  func stop() {
    stopCount += 1
    setState(.disconnected)
  }

  func sendMessage(text: String, receiverId: UUID) async throws -> LocalMessage {
    requests.append(ChatSendRequest(text: text, receiverId: receiverId))
    switch sendResponse {
    case .success(let message):
      return message
    case .failure(let failure):
      throw failure
    case .suspended:
      try await Task.sleep(for: .seconds(60))
      throw CancellationError()
    }
  }

  func connectionState() -> MessagingConnectionState { state }

  func observeConnectionState() -> AsyncStream<MessagingConnectionState> {
    AsyncStream { continuation in
      observers.append(continuation)
      continuation.yield(state)
    }
  }

  func observeIssues() -> AsyncStream<MessagingIssue> {
    AsyncStream { continuation in issueObservers.append(continuation) }
  }

  func emit(_ state: MessagingConnectionState) {
    setState(state)
  }

  func emitIssue(_ issue: MessagingIssue) {
    for observer in issueObservers { observer.yield(issue) }
  }

  private func setState(_ newState: MessagingConnectionState) {
    state = newState
    for observer in observers { observer.yield(newState) }
  }
}

private enum ChatIDs {
  static let current = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!
  static let peer = UUID(uuidString: "22222222-2222-4222-8222-222222222222")!
  static let message = UUID(uuidString: "33333333-3333-4333-8333-333333333333")!
  static let conversation = "\(current.uuidString.lowercased()):\(peer.uuidString.lowercased())"
}

private func makeLocalMessage(
  text: String,
  state: MessageState
) throws -> LocalMessage {
  LocalMessage(
    message: try MessageDTO(
      messageId: ChatIDs.message,
      text: text,
      senderId: ChatIDs.current,
      receiverId: ChatIDs.peer,
      clientCreatedAt: Date(timeIntervalSince1970: 1_700_000_000),
      clientSequence: 1
    ),
    direction: .outgoing,
    state: state,
    receivedAt: nil
  )
}

@MainActor
private func eventually(
  timeout: Duration = .seconds(1),
  condition: @escaping @MainActor () async -> Bool
) async throws {
  let clock = ContinuousClock()
  let deadline = clock.now.advanced(by: timeout)
  while !(await condition()) {
    guard clock.now < deadline else {
      Issue.record("Condition was not met before timeout")
      return
    }
    await Task.yield()
  }
}
// MARK: - AI Generated - End
