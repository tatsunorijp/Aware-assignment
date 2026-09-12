import Foundation

nonisolated enum WebSocketFrame: Equatable, Sendable {
  case text(String)
  case binary(Data)
}

nonisolated struct WebSocketCloseDetails: Equatable, Sendable {
  let code: Int
  let reason: String?
}

nonisolated protocol WebSocketTransport: AnyObject, Sendable {
  var closeDetails: WebSocketCloseDetails? { get }

  func start()
  func send(text: String) async throws
  func receive() async throws -> WebSocketFrame
  func cancel(code: URLSessionWebSocketTask.CloseCode, reason: Data?)
}

nonisolated protocol WebSocketTransportFactory: Sendable {
  func makeTransport(url: URL) -> any WebSocketTransport
}

nonisolated final class URLSessionWebSocketTransport: WebSocketTransport, @unchecked Sendable {
  private let task: URLSessionWebSocketTask

  init(task: URLSessionWebSocketTask) {
    self.task = task
  }

  var closeDetails: WebSocketCloseDetails? {
    guard task.closeCode != .invalid else { return nil }
    return WebSocketCloseDetails(
      code: task.closeCode.rawValue,
      reason: task.closeReason.flatMap { String(data: $0, encoding: .utf8) }
    )
  }

  func start() {
    task.resume()
  }

  func send(text: String) async throws {
    try await task.send(.string(text))
  }

  func receive() async throws -> WebSocketFrame {
    switch try await task.receive() {
    case .string(let text):
      return .text(text)
    case .data(let data):
      return .binary(data)
    @unknown default:
      throw NetworkError.invalidWebSocketFrame
    }
  }

  func cancel(code: URLSessionWebSocketTask.CloseCode, reason: Data?) {
    task.cancel(with: code, reason: reason)
  }
}

nonisolated struct URLSessionWebSocketTransportFactory: WebSocketTransportFactory, Sendable {
  let session: URLSession

  init(session: URLSession = .shared) {
    self.session = session
  }

  func makeTransport(url: URL) -> any WebSocketTransport {
    URLSessionWebSocketTransport(task: session.webSocketTask(with: url))
  }
}
