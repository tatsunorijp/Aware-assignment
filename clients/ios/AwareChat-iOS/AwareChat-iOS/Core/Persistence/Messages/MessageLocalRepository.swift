import Foundation

nonisolated enum MessageDirection: String, Sendable { case outgoing, incoming }
nonisolated enum MessageState: String, Sendable { case pendingToSend, sending, sent, failed }

nonisolated struct LocalMessage: Equatable, Sendable, Identifiable {
  let message: MessageDTO
  let direction: MessageDirection
  let state: MessageState?
  let receivedAt: Date?
  var id: UUID { message.messageId }
  var displayTimestamp: Date { receivedAt ?? message.clientCreatedAt }

  /// Local acceptance metadata must never be echoed in a send_message request.
  func outgoingPayload() throws -> MessageDTO {
    guard direction == .outgoing else { throw PersistenceError.invalidData }
    return try MessageDTO(
      messageId: message.messageId, text: message.text, senderId: message.senderId,
      receiverId: message.receiverId, clientCreatedAt: message.clientCreatedAt,
      clientSequence: message.clientSequence
    )
  }
}

@MainActor
protocol MessageLocalRepository: Sendable {
  func message(id: UUID) throws -> LocalMessage?
  func messages(conversationId: String) throws -> [LocalMessage]
  func pendingMessages() throws -> [LocalMessage]
  @discardableResult func enqueue(text: String, receiverId: UUID, createdAt: Date) throws -> LocalMessage
  @discardableResult func persistIncoming(_ message: MessageDTO, receivedAt: Date) throws -> LocalMessage
  func markSending(id: UUID) throws
  func markAccepted(id: UUID, serverReceivedAt: Date) throws
  func markRejected(id: UUID, retryable: Bool) throws
  func recoverInterruptedSends() throws
  func observeMessages(conversationId: String) -> AsyncThrowingStream<[LocalMessage], any Error>
}
