import Foundation

nonisolated enum ProtocolConstants {
  static let version = 1
}

nonisolated enum ClientEvent: Equatable, Sendable {
  case identify(user: UserDTO)
  case sendMessage(message: MessageDTO)
  case messagePersisted(messageId: UUID)
}

nonisolated extension ClientEvent: Encodable {
  private enum CodingKeys: String, CodingKey {
    case type
    case protocolVersion
    case user
    case message
    case messageId
  }

  func encode(to encoder: any Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(ProtocolConstants.version, forKey: .protocolVersion)
    switch self {
    case .identify(let user):
      try container.encode("identify", forKey: .type)
      try container.encode(user, forKey: .user)
    case .sendMessage(let message):
      try container.encode("send_message", forKey: .type)
      try container.encode(message, forKey: .message)
    case .messagePersisted(let messageId):
      try container.encode("message_persisted", forKey: .type)
      try container.encode(WireValidation.normalized(messageId), forKey: .messageId)
    }
  }
}

nonisolated struct ProtocolErrorEvent: Equatable, Sendable {
  let messageId: String?
  let error: ServerError
}

nonisolated enum ServerEvent: Equatable, Sendable {
  case identityAccepted(user: UserDTO)
  case syncCompleted(pendingCount: Int)
  case messageAccepted(messageId: UUID, serverReceivedAt: Date)
  case incomingMessage(message: MessageDTO)
  case protocolError(ProtocolErrorEvent)
}

nonisolated struct ProtocolCodec: Sendable {
  func encode(_ event: ClientEvent) throws -> Data {
    try JSONEncoder().encode(event)
  }

  func decodeServerEvent(from data: Data) throws -> ServerEvent {
    let decoder = JSONDecoder()
    let header = try decoder.decode(EventHeader.self, from: data)
    guard header.protocolVersion == ProtocolConstants.version else {
      throw WireModelError.invalidProtocolVersion
    }

    switch header.type {
    case "identity_accepted":
      let envelope = try decoder.decode(IdentityAcceptedEnvelope.self, from: data)
      return .identityAccepted(user: envelope.user)
    case "sync_completed":
      let envelope = try decoder.decode(SyncCompletedEnvelope.self, from: data)
      guard envelope.pendingCount >= 0 else { throw WireModelError.invalidPendingCount }
      return .syncCompleted(pendingCount: envelope.pendingCount)
    case "message_accepted":
      let envelope = try decoder.decode(MessageAcceptedEnvelope.self, from: data)
      return .messageAccepted(
        messageId: try WireValidation.uuid(from: envelope.messageId),
        serverReceivedAt: try WireDateCodec.decode(envelope.serverReceivedAt)
      )
    case "incoming_message":
      let envelope = try decoder.decode(IncomingMessageEnvelope.self, from: data)
      guard envelope.message.serverReceivedAt != nil else {
        throw WireModelError.missingServerTimestamp
      }
      return .incomingMessage(message: envelope.message)
    case "protocol_error":
      let envelope = try decoder.decode(ProtocolErrorEnvelope.self, from: data)
      return .protocolError(
        ProtocolErrorEvent(messageId: envelope.messageId, error: envelope.error)
      )
    default:
      throw DecodingError.dataCorrupted(
        .init(codingPath: [], debugDescription: "Unsupported server event type")
      )
    }
  }
}

private nonisolated struct EventHeader: Decodable {
  let type: String
  let protocolVersion: Int
}

private nonisolated struct IdentityAcceptedEnvelope: Decodable {
  let user: UserDTO
}

private nonisolated struct SyncCompletedEnvelope: Decodable {
  let pendingCount: Int
}

private nonisolated struct MessageAcceptedEnvelope: Decodable {
  let messageId: String
  let serverReceivedAt: String
}

private nonisolated struct IncomingMessageEnvelope: Decodable {
  let message: MessageDTO
}

private nonisolated struct ProtocolErrorEnvelope: Decodable {
  let messageId: String?
  let error: ServerError
}
