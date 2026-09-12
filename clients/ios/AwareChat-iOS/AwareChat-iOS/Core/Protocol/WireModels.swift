import Foundation

nonisolated enum WireModelError: Error, Equatable, Sendable {
  case invalidUUID
  case blankValue
  case invalidConversation
  case invalidSequence
  case invalidDate
  case invalidProtocolVersion
  case invalidHealthResponse
  case invalidPendingCount
  case missingServerTimestamp
}

nonisolated enum WireValidation {
  static func uuid(from value: String) throws -> UUID {
    let hyphenIndexes = [8, 13, 18, 23]
    guard value.utf8.count == 36,
          hyphenIndexes.allSatisfy({ index in
            value.utf8[value.utf8.index(value.utf8.startIndex, offsetBy: index)] == 45
          }),
          let uuid = UUID(uuidString: value) else {
      throw WireModelError.invalidUUID
    }
    return uuid
  }

  static func nonblank(_ value: String) throws -> String {
    guard value.contains(where: { !$0.isWhitespace }) else {
      throw WireModelError.blankValue
    }
    return value
  }

  static func normalized(_ uuid: UUID) -> String {
    uuid.uuidString.lowercased()
  }

  static func conversationID(first: UUID, second: UUID) throws -> String {
    guard first != second else { throw WireModelError.invalidConversation }
    return [normalized(first), normalized(second)].sorted().joined(separator: ":")
  }
}

nonisolated struct UserDTO: Equatable, Sendable {
  let userId: UUID
  let name: String

  init(userId: UUID, name: String) throws {
    self.userId = userId
    self.name = try WireValidation.nonblank(name)
  }
}

nonisolated extension UserDTO: Codable {
  private enum CodingKeys: String, CodingKey {
    case userId
    case name
  }

  init(from decoder: any Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    userId = try WireValidation.uuid(from: container.decode(String.self, forKey: .userId))
    name = try WireValidation.nonblank(container.decode(String.self, forKey: .name))
  }

  func encode(to encoder: any Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(WireValidation.normalized(userId), forKey: .userId)
    try container.encode(name, forKey: .name)
  }
}

nonisolated struct MessageDTO: Equatable, Sendable {
  let messageId: UUID
  let conversationId: String
  let text: String
  let senderId: UUID
  let receiverId: UUID
  let clientCreatedAt: Date
  let clientSequence: Int64
  let serverReceivedAt: Date?

  init(
    messageId: UUID,
    text: String,
    senderId: UUID,
    receiverId: UUID,
    clientCreatedAt: Date,
    clientSequence: Int64,
    serverReceivedAt: Date? = nil
  ) throws {
    guard clientSequence > 0 else { throw WireModelError.invalidSequence }
    self.messageId = messageId
    self.conversationId = try WireValidation.conversationID(first: senderId, second: receiverId)
    self.text = try WireValidation.nonblank(text)
    self.senderId = senderId
    self.receiverId = receiverId
    self.clientCreatedAt = clientCreatedAt
    self.clientSequence = clientSequence
    self.serverReceivedAt = serverReceivedAt
  }
}

nonisolated extension MessageDTO: Codable {
  private enum CodingKeys: String, CodingKey {
    case messageId
    case conversationId
    case text
    case senderId
    case receiverId
    case clientCreatedAt
    case clientSequence
    case serverReceivedAt
  }

  init(from decoder: any Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    let messageId = try WireValidation.uuid(
      from: container.decode(String.self, forKey: .messageId)
    )
    let senderId = try WireValidation.uuid(
      from: container.decode(String.self, forKey: .senderId)
    )
    let receiverId = try WireValidation.uuid(
      from: container.decode(String.self, forKey: .receiverId)
    )
    let conversationId = try container.decode(String.self, forKey: .conversationId)
    let expectedConversationId = try WireValidation.conversationID(
      first: senderId,
      second: receiverId
    )
    guard conversationId == expectedConversationId else {
      throw WireModelError.invalidConversation
    }

    let sequence = try container.decode(Int64.self, forKey: .clientSequence)
    guard sequence > 0 else { throw WireModelError.invalidSequence }

    self.messageId = messageId
    self.conversationId = conversationId
    self.text = try WireValidation.nonblank(container.decode(String.self, forKey: .text))
    self.senderId = senderId
    self.receiverId = receiverId
    self.clientCreatedAt = try WireDateCodec.decode(
      container.decode(String.self, forKey: .clientCreatedAt)
    )
    self.clientSequence = sequence
    if let timestamp = try container.decodeIfPresent(String.self, forKey: .serverReceivedAt) {
      self.serverReceivedAt = try WireDateCodec.decode(timestamp)
    } else {
      self.serverReceivedAt = nil
    }
  }

  func encode(to encoder: any Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(WireValidation.normalized(messageId), forKey: .messageId)
    try container.encode(conversationId, forKey: .conversationId)
    try container.encode(text, forKey: .text)
    try container.encode(WireValidation.normalized(senderId), forKey: .senderId)
    try container.encode(WireValidation.normalized(receiverId), forKey: .receiverId)
    try container.encode(WireDateCodec.encode(clientCreatedAt), forKey: .clientCreatedAt)
    try container.encode(clientSequence, forKey: .clientSequence)
    if let serverReceivedAt {
      try container.encode(WireDateCodec.encode(serverReceivedAt), forKey: .serverReceivedAt)
    } else {
      try container.encodeNil(forKey: .serverReceivedAt)
    }
  }
}

nonisolated struct HealthResponse: Decodable, Equatable, Sendable {
  let status: String
  let protocolVersion: Int

  init(from decoder: any Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    status = try container.decode(String.self, forKey: .status)
    protocolVersion = try container.decode(Int.self, forKey: .protocolVersion)
    guard status == "ok", protocolVersion == ProtocolConstants.version else {
      throw WireModelError.invalidHealthResponse
    }
  }

  private enum CodingKeys: String, CodingKey {
    case status
    case protocolVersion
  }
}

nonisolated struct UsersResponse: Decodable, Equatable, Sendable {
  let users: [UserDTO]
}

nonisolated struct HTTPErrorEnvelope: Decodable, Equatable, Sendable {
  let error: ServerError
}
