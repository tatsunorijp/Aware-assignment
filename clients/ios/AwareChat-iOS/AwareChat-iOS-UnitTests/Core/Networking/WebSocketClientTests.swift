// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("WebSocket Client")
struct WebSocketClientTests {
  private let configuration = NetworkConfiguration(
    httpBaseURL: URL(string: "http://example.test")!,
    webSocketURL: URL(string: "ws://example.test/ws")!
  )

  @Test
  func sendBeforeConnectFails() async {
    let transport = MockWebSocketTransport()
    let client = WebSocketClient(
      configuration: configuration,
      factory: MockWebSocketTransportFactory(transport: transport)
    )

    await #expect(throws: NetworkError.notConnected) {
      try await client.send(.messagePersisted(messageId: UUID()))
    }
  }

  @Test
  func connectReceivesTextEventAndSendEncodesClientEvent() async throws {
    let transport = MockWebSocketTransport()
    await transport.enqueue(
      .frame(
        .text(
          #"{"type":"identity_accepted","protocolVersion":1,"user":{"userId":"11111111-1111-4111-8111-111111111111","name":"Alice"}}"#
        )
      )
    )
    let client = WebSocketClient(
      configuration: configuration,
      factory: MockWebSocketTransportFactory(transport: transport)
    )

    let stream = await client.connect()
    var iterator = stream.makeAsyncIterator()
    let event = try await iterator.next()
    let user = try UserDTO(
      userId: UUID(uuidString: "11111111-1111-4111-8111-111111111111")!,
      name: "Alice"
    )
    try await client.send(.identify(user: user))

    #expect(event == .identityAccepted(user: user))
    #expect(await client.connectionState() == .connected)
    #expect(await transport.hasStarted())

    let sentTexts = await transport.sentTexts()
    #expect(sentTexts.count == 1)
    let text = try #require(sentTexts.first)
    let object = try #require(
      JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any]
    )
    #expect(object["type"] as? String == "identify")
    #expect(object["protocolVersion"] as? Int == 1)

    await client.disconnect()
  }

  @Test
  func binaryFrameFailsStreamAndConnection() async throws {
    let transport = MockWebSocketTransport()
    await transport.enqueue(.frame(.binary(Data([0x01]))))
    let client = WebSocketClient(
      configuration: configuration,
      factory: MockWebSocketTransportFactory(transport: transport)
    )

    let stream = await client.connect()
    var iterator = stream.makeAsyncIterator()

    await #expect(throws: NetworkError.invalidWebSocketFrame) {
      _ = try await iterator.next()
    }
    #expect(await client.connectionState() == .connectionFailure)
    #expect(await transport.hasBeenCancelled())
  }

  @Test
  func closeDetailsAreReported() async throws {
    let transport = MockWebSocketTransport(
      closeDetails: WebSocketCloseDetails(code: 4001, reason: "Session replaced")
    )
    await transport.enqueue(.failure(.transportFailure))
    let client = WebSocketClient(
      configuration: configuration,
      factory: MockWebSocketTransportFactory(transport: transport)
    )

    let stream = await client.connect()
    var iterator = stream.makeAsyncIterator()

    await #expect(
      throws: NetworkError.webSocketClosed(code: 4001, reason: "Session replaced")
    ) {
      _ = try await iterator.next()
    }
  }

  @Test
  func disconnectFinishesStreamAndResetsState() async throws {
    let transport = MockWebSocketTransport()
    let client = WebSocketClient(
      configuration: configuration,
      factory: MockWebSocketTransportFactory(transport: transport)
    )

    let stream = await client.connect()
    await client.disconnect()
    var iterator = stream.makeAsyncIterator()

    #expect(try await iterator.next() == nil)
    #expect(await client.connectionState() == .disconnected)
    #expect(await transport.hasBeenCancelled())
  }

  @Test
  func cancelledOldReceiveCannotPublishIntoReplacementStream() async throws {
    let transport = MockWebSocketTransport(cancelResumesReceivers: false)
    let client = WebSocketClient(
      configuration: configuration, factory: MockWebSocketTransportFactory(transport: transport)
    )
    _ = await client.connect()
    try await eventually { await transport.waitingReceiverCount() == 1 }
    let replacement = await client.connect()
    try await eventually { await transport.waitingReceiverCount() == 2 }
    await transport.enqueue(.frame(.text(#"{"type":"sync_completed","protocolVersion":1,"pendingCount":99}"#)))
    await transport.enqueue(.frame(.text(#"{"type":"sync_completed","protocolVersion":1,"pendingCount":0}"#)))
    var events = replacement.makeAsyncIterator()
    #expect(try await events.next() == .syncCompleted(pendingCount: 0))
    await client.disconnect()
    await transport.enqueue(.failure(.cancelled))
  }

  @Test
  func cancelledSendDoesNotReachTheTransport() async throws {
    let transport = MockWebSocketTransport()
    let client = WebSocketClient(
      configuration: configuration, factory: MockWebSocketTransportFactory(transport: transport)
    )
    _ = await client.connect()
    let task = Task {
      withUnsafeCurrentTask { $0?.cancel() }
      await #expect(throws: NetworkError.cancelled) {
        try await client.send(.messagePersisted(messageId: UUID()))
      }
    }
    await task.value
    #expect(await transport.sentTexts().isEmpty)
    await client.disconnect()
  }
}

private struct MockWebSocketTransportFactory: WebSocketTransportFactory {
  let transport: MockWebSocketTransport

  func makeTransport(url: URL) -> any WebSocketTransport {
    transport
  }
}

private final class MockWebSocketTransport: WebSocketTransport, @unchecked Sendable {
  let closeDetails: WebSocketCloseDetails?
  private let state: MockWebSocketTransportState

  init(closeDetails: WebSocketCloseDetails? = nil, cancelResumesReceivers: Bool = true) {
    self.closeDetails = closeDetails
    state = MockWebSocketTransportState(cancelResumesReceivers: cancelResumesReceivers)
  }

  func start() {
    Task { await state.markStarted() }
  }

  func send(text: String) async throws {
    await state.record(text: text)
  }

  func receive() async throws -> WebSocketFrame {
    switch await state.nextReceive() {
    case .frame(let frame):
      return frame
    case .failure(let error):
      throw error
    }
  }

  func cancel(code: URLSessionWebSocketTask.CloseCode, reason: Data?) {
    Task { await state.cancel() }
  }

  func enqueue(_ receive: MockWebSocketReceive) async {
    await state.enqueue(receive)
  }

  func sentTexts() async -> [String] {
    await state.sentTexts
  }

  func hasStarted() async -> Bool {
    await state.started
  }

  func hasBeenCancelled() async -> Bool {
    await state.cancelled
  }

  func waitingReceiverCount() async -> Int { await state.waitingReceiverCount }
}

private enum MockWebSocketReceive: Sendable {
  case frame(WebSocketFrame)
  case failure(NetworkError)
}

private actor MockWebSocketTransportState {
  private let cancelResumesReceivers: Bool
  var waitingReceiverCount: Int { waiters.count }
  private(set) var sentTexts = [String]()
  private(set) var started = false
  private(set) var cancelled = false
  private var receives = [MockWebSocketReceive]()
  private var waiters = [CheckedContinuation<MockWebSocketReceive, Never>]()

  init(cancelResumesReceivers: Bool) { self.cancelResumesReceivers = cancelResumesReceivers }

  func markStarted() {
    started = true
  }

  func record(text: String) {
    sentTexts.append(text)
  }

  func enqueue(_ receive: MockWebSocketReceive) {
    if waiters.isEmpty {
      receives.append(receive)
    } else {
      waiters.removeFirst().resume(returning: receive)
    }
  }

  func nextReceive() async -> MockWebSocketReceive {
    if !receives.isEmpty {
      return receives.removeFirst()
    }
    return await withCheckedContinuation { continuation in
      waiters.append(continuation)
    }
  }

  func cancel() {
    cancelled = true
    guard cancelResumesReceivers else { return }
    let pendingWaiters = waiters
    waiters.removeAll()
    pendingWaiters.forEach { $0.resume(returning: .failure(.cancelled)) }
  }
}
// MARK: - AI Generated - End
