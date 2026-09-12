import Foundation

nonisolated struct ServerError: Error, Equatable, Sendable {
  let code: String
  let userMessage: String
  let developerMessage: String?
  let isRetryable: Bool
  let requestId: String?

  init(
    code: String,
    userMessage: String,
    developerMessage: String? = nil,
    isRetryable: Bool,
    requestId: String? = nil
  ) throws {
    self.code = try WireValidation.nonblank(code)
    self.userMessage = try WireValidation.nonblank(userMessage)
    self.developerMessage = developerMessage
    self.isRetryable = isRetryable
    self.requestId = requestId
  }
}

nonisolated extension ServerError: Codable {
  private enum CodingKeys: String, CodingKey {
    case code
    case userMessage
    case developerMessage
    case isRetryable
    case requestId
  }

  init(from decoder: any Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    code = try WireValidation.nonblank(container.decode(String.self, forKey: .code))
    userMessage = try WireValidation.nonblank(
      container.decode(String.self, forKey: .userMessage)
    )
    developerMessage = try container.decodeIfPresent(String.self, forKey: .developerMessage)
    isRetryable = try container.decode(Bool.self, forKey: .isRetryable)
    requestId = try container.decodeIfPresent(String.self, forKey: .requestId)
  }

  func encode(to encoder: any Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(code, forKey: .code)
    try container.encode(userMessage, forKey: .userMessage)
    try container.encodeIfPresent(developerMessage, forKey: .developerMessage)
    try container.encode(isRetryable, forKey: .isRetryable)
    try container.encodeIfPresent(requestId, forKey: .requestId)
  }
}

nonisolated extension ServerError: LocalizedError {
  var errorDescription: String? {
    userMessage
  }
}
