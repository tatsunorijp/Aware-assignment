import Foundation

nonisolated enum WebSocketConnectionState: Equatable, Sendable {
  case disconnected
  case connecting
  case connected
  case connectionFailure
}

protocol WebSocketClientProtocol: Sendable {
  func connect() async -> AsyncThrowingStream<ServerEvent, any Error>
  func send(_ event: ClientEvent) async throws
  func disconnect() async
  func connectionState() async -> WebSocketConnectionState
}

actor WebSocketClient: WebSocketClientProtocol {
  private let configuration: NetworkConfiguration
  private let factory: any WebSocketTransportFactory
  private let codec: ProtocolCodec

  private var state = WebSocketConnectionState.disconnected
  private var transport: (any WebSocketTransport)?
  private var receiveTask: Task<Void, Never>?
  private var continuation: AsyncThrowingStream<ServerEvent, any Error>.Continuation?

  init(
    configuration: NetworkConfiguration,
    factory: any WebSocketTransportFactory = URLSessionWebSocketTransportFactory(),
    codec: ProtocolCodec = ProtocolCodec()
  ) {
    self.configuration = configuration
    self.factory = factory
    self.codec = codec
  }

  func connect() -> AsyncThrowingStream<ServerEvent, any Error> {
    disconnectCurrentTransport()
    state = .connecting

    let transport = factory.makeTransport(url: configuration.webSocketURL)
    self.transport = transport

    let stream = AsyncThrowingStream<ServerEvent, any Error> { continuation in
      self.continuation = continuation
    }

    transport.start()
    state = .connected
    receiveTask = Task { [weak self] in
      await self?.receiveLoop(from: transport)
    }
    return stream
  }

  func send(_ event: ClientEvent) async throws {
    guard !Task.isCancelled else { throw NetworkError.cancelled }
    guard state == .connected, let transport else {
      throw NetworkError.notConnected
    }

    let data: Data
    do {
      data = try codec.encode(event)
    } catch {
      throw NetworkError.encoding
    }

    guard let text = String(data: data, encoding: .utf8) else {
      throw NetworkError.encoding
    }

    do {
      try await transport.send(text: text)
    } catch {
      throw NetworkError.wrapping(error)
    }
  }

  func disconnect() {
    disconnectCurrentTransport()
    state = .disconnected
  }

  func connectionState() -> WebSocketConnectionState {
    state
  }

  private func receiveLoop(from activeTransport: any WebSocketTransport) async {
    do {
      while !Task.isCancelled {
        let frame = try await activeTransport.receive()
        guard !Task.isCancelled, let transport, transport === activeTransport else { return }
        guard case .text(let text) = frame,
              let data = text.data(using: .utf8) else {
          throw NetworkError.invalidWebSocketFrame
        }
        let event: ServerEvent
        do {
          event = try codec.decodeServerEvent(from: data)
        } catch {
          throw NetworkError.decoding
        }
        continuation?.yield(event)
      }
    } catch {
      guard !Task.isCancelled, let transport, transport === activeTransport else { return }
      let networkError: NetworkError
      if let close = activeTransport.closeDetails {
        networkError = .webSocketClosed(code: close.code, reason: close.reason)
      } else {
        networkError = NetworkError.wrapping(error)
      }
      state = .connectionFailure
      continuation?.finish(throwing: networkError)
      activeTransport.cancel(code: .goingAway, reason: nil)
      clear(activeTransport: activeTransport)
    }
  }

  private func disconnectCurrentTransport() {
    receiveTask?.cancel()
    receiveTask = nil
    transport?.cancel(code: .normalClosure, reason: nil)
    transport = nil
    continuation?.finish()
    continuation = nil
  }

  private func clear(activeTransport: any WebSocketTransport) {
    guard let transport, transport === activeTransport else { return }
    self.transport = nil
    receiveTask = nil
    continuation = nil
  }
}
