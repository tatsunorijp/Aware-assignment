// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

actor TestMessagingSocket: WebSocketClientProtocol {
  private var continuation: AsyncThrowingStream<ServerEvent, any Error>.Continuation?
  private(set) var connections = 0
  private(set) var sent: [ClientEvent] = []
  private(set) var disconnected = true
  var failNextSend = false

  func connect() -> AsyncThrowingStream<ServerEvent, any Error> {
    connections += 1
    disconnected = false
    let (stream, continuation) = AsyncThrowingStream<ServerEvent, any Error>.makeStream()
    self.continuation = continuation
    return stream
  }

  func send(_ event: ClientEvent) throws {
    try Task.checkCancellation()
    guard !disconnected else { throw NetworkError.notConnected }
    if failNextSend {
      failNextSend = false
      throw NetworkError.transportFailure
    }
    sent.append(event)
  }

  func disconnect() {
    disconnected = true
    continuation?.finish()
    continuation = nil
  }

  func connectionState() -> WebSocketConnectionState { disconnected ? .disconnected : .connected }
  func emit(_ event: ServerEvent) { continuation?.yield(event) }
  func loseConnection(_ error: NetworkError = .transportFailure) { continuation?.finish(throwing: error) }
  func rejectNextWrite() { failNextSend = true }
  func sentMessages() -> [MessageDTO] {
    sent.compactMap { if case .sendMessage(let message) = $0 { message } else { nil } }
  }
  func acknowledgementCount(id: UUID) -> Int {
    sent.filter { $0 == .messagePersisted(messageId: id) }.count
  }
}

actor TestMessagingClock: MessagingClock {
  private struct Waiter {
    let duration: Duration
    let continuation: CheckedContinuation<Void, any Error>
  }
  private var waiters: [UUID: Waiter] = [:]
  private(set) var requested: [Duration] = []

  nonisolated func now() -> Date { Date(timeIntervalSince1970: 1_000) }

  func sleep(for duration: Duration) async throws {
    let id = UUID()
    try await withTaskCancellationHandler {
      try Task.checkCancellation()
      try await withCheckedThrowingContinuation { continuation in
        requested.append(duration)
        waiters[id] = Waiter(duration: duration, continuation: continuation)
      }
    } onCancel: {
      Task { await self.cancel(id) }
    }
  }

  func hasWaiter(for duration: Duration) -> Bool { waiters.values.contains { $0.duration == duration } }

  func advance(_ duration: Duration) {
    let matching = waiters.filter { $0.value.duration == duration }
    for (id, waiter) in matching {
      waiters.removeValue(forKey: id)
      waiter.continuation.resume()
    }
  }

  private func cancel(_ id: UUID) {
    waiters.removeValue(forKey: id)?.continuation.resume(throwing: CancellationError())
  }
}

@MainActor
final class MessagingTestHarness {
  let store: PersistenceTestStore
  let socket = TestMessagingSocket()
  let clock = TestMessagingClock()
  let service: MessagingService
  let user: UserDTO

  init() throws {
    store = try PersistenceTestStore()
    user = try store.identify()
    service = MessagingService(socket: socket, users: store.users, messages: store.messages, clock: clock)
  }

  func connect() async throws {
    let previousIdentifications = await socket.sent.filter { $0 == .identify(user: user) }.count
    await service.start()
    try await eventually {
      await self.socket.sent.filter { $0 == .identify(user: self.user) }.count > previousIdentifications
    }
    await socket.emit(.identityAccepted(user: user))
    try await eventually { await self.service.connectionState() == .connected }
  }

  func advance(_ duration: Duration) async throws {
    try await eventually { await self.clock.hasWaiter(for: duration) }
    await clock.advance(duration)
  }
}

// MARK: - AI Generated - End
