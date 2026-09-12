import Foundation

nonisolated enum MessagingFailure: Error, Equatable, Sendable, LocalizedError {
  case network(NetworkError)
  case storage(PersistenceError)
  case server(ServerError)
  case identificationTimedOut
  case acceptanceTimedOut
  case unexpectedIdentity
  case sessionReplaced

  var errorDescription: String? {
    switch self {
    case .network(let error): error.localizedDescription
    case .storage(let error): error.localizedDescription
    case .server(let error): error.userMessage
    case .identificationTimedOut: "The server did not confirm your name. Please try again."
    case .acceptanceTimedOut: "The message is saved and waiting for another send attempt."
    case .unexpectedIdentity: "The server returned a different identity. Please reconnect."
    case .sessionReplaced: "This user connected on another session. Reconnect when you are ready."
    }
  }
}

nonisolated enum MessagingConnectionState: Equatable, Sendable {
  case disconnected
  case connecting
  /// Identification AND the local registration-completion save succeeded.
  case connected
  case connectionFailure(MessagingFailure)
}

/// A transient operation error, independent of connection and persisted message state.
nonisolated struct MessagingIssue: Equatable, Sendable {
  let messageId: UUID?
  let error: ServerError
}

protocol MessagingServiceProtocol: Sendable {
  func start() async
  func stop() async
  @discardableResult func sendMessage(text: String, receiverId: UUID) async throws -> LocalMessage
  func connectionState() async -> MessagingConnectionState
  func observeConnectionState() async -> AsyncStream<MessagingConnectionState>
  func observeIssues() async -> AsyncStream<MessagingIssue>
}
