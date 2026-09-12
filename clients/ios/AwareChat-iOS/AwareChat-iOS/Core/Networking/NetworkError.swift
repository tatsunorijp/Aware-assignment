import Foundation

nonisolated enum NetworkError: Error, Equatable, Sendable {
  case invalidHTTPResponse
  case invalidErrorResponse(statusCode: Int)
  case server(statusCode: Int, error: ServerError)
  case transport(code: URLError.Code)
  case transportFailure
  case encoding
  case decoding
  case notConnected
  case invalidWebSocketFrame
  case webSocketClosed(code: Int, reason: String?)
  case cancelled
}

nonisolated extension NetworkError: LocalizedError {
  var errorDescription: String? {
    switch self {
    case .server(_, let error):
      error.userMessage
    case .cancelled:
      "The operation was cancelled."
    default:
      "Something went wrong. Please try again."
    }
  }

  static func wrapping(_ error: any Error) -> NetworkError {
    if let networkError = error as? NetworkError {
      return networkError
    }
    if error is CancellationError {
      return .cancelled
    }
    if let urlError = error as? URLError {
      return .transport(code: urlError.code)
    }
    return .transportFailure
  }
}
