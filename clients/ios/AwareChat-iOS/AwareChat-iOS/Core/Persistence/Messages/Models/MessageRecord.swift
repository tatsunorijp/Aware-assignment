import Foundation
import SwiftData

@Model
final class MessageRecord {
  @Attribute(.unique) var messageId: UUID
  var conversationId: String
  var text: String
  var senderId: UUID
  var receiverId: UUID
  var clientCreatedAt: Date
  var clientSequence: Int64
  var serverReceivedAt: Date?
  var receivedAt: Date?
  var displayTimestamp: Date
  var directionRawValue: String
  var stateRawValue: String?

  init(message: MessageDTO, direction: MessageDirection, state: MessageState?, receivedAt: Date?) {
    messageId = message.messageId
    conversationId = message.conversationId
    text = message.text
    senderId = message.senderId
    receiverId = message.receiverId
    clientCreatedAt = message.clientCreatedAt
    clientSequence = message.clientSequence
    serverReceivedAt = message.serverReceivedAt
    self.receivedAt = receivedAt
    displayTimestamp = receivedAt ?? message.clientCreatedAt
    directionRawValue = direction.rawValue
    stateRawValue = state?.rawValue
  }

  func value() throws -> LocalMessage {
    guard let direction = MessageDirection(rawValue: directionRawValue) else {
      throw PersistenceError.invalidData
    }
    let state = stateRawValue.flatMap(MessageState.init(rawValue:))
    guard (direction == .outgoing && state != nil && receivedAt == nil)
      || (direction == .incoming && stateRawValue == nil && receivedAt != nil) else {
      throw PersistenceError.invalidData
    }
    let message = try MessageDTO(
      messageId: messageId, text: text, senderId: senderId, receiverId: receiverId,
      clientCreatedAt: clientCreatedAt, clientSequence: clientSequence, serverReceivedAt: serverReceivedAt
    )
    guard message.conversationId == conversationId else { throw PersistenceError.invalidData }
    return LocalMessage(message: message, direction: direction, state: state, receivedAt: receivedAt)
  }
}
