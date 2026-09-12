import Foundation
import SwiftData

@MainActor
final class SwiftDataMessageLocalRepository: MessageLocalRepository {
  private let database: PersistenceContainer

  init(database: PersistenceContainer) { self.database = database }

  func message(id: UUID) throws -> LocalMessage? {
    try database.read { try database.messageRecord(id)?.value() }
  }

  func messages(conversationId: String) throws -> [LocalMessage] {
    try database.read { try database.messageRecords(conversationId: conversationId).map { try $0.value() } }
  }

  func pendingMessages() throws -> [LocalMessage] {
    try database.read {
      let pending = MessageState.pendingToSend.rawValue
      let outgoing = MessageDirection.outgoing.rawValue
      return try database.context.fetch(FetchDescriptor<MessageRecord>(
        predicate: #Predicate { $0.directionRawValue == outgoing && $0.stateRawValue == pending },
        sortBy: [SortDescriptor(\MessageRecord.clientSequence)]
      )).map { try $0.value() }
    }
  }

  @discardableResult
  func enqueue(text: String, receiverId: UUID, createdAt: Date) throws -> LocalMessage {
    try database.write {
      guard let current = try database.currentIdentityRecord() else {
        throw PersistenceError.missingIdentity
      }
      guard current.lastClientSequence < Int64.max else { throw PersistenceError.sequenceExhausted }
      let message = try MessageDTO(
        messageId: UUID(), text: text, senderId: current.userId, receiverId: receiverId,
        clientCreatedAt: createdAt, clientSequence: current.lastClientSequence + 1
      )
      _ = try database.ensureConversation(first: current.userId, second: receiverId)
      let record = MessageRecord(message: message, direction: .outgoing, state: .pendingToSend, receivedAt: nil)
      database.context.insert(record)
      current.lastClientSequence = message.clientSequence
      return try record.value()
    }
  }

  @discardableResult
  func persistIncoming(_ message: MessageDTO, receivedAt: Date) throws -> LocalMessage {
    try database.write {
      guard let current = try database.currentIdentityRecord() else {
        throw PersistenceError.missingIdentity
      }
      guard message.receiverId == current.userId, message.serverReceivedAt != nil else {
        throw PersistenceError.invalidData
      }
      if let existing = try database.messageRecord(message.messageId) {
        let value = try existing.value()
        let saved = value.message
        // A volatile server can assign a new acceptance time after a restart.
        // Deduplicate immutable client content, retaining the first local receipt.
        guard value.direction == .incoming,
              saved.text == message.text, saved.senderId == message.senderId,
              saved.receiverId == message.receiverId,
              saved.clientCreatedAt == message.clientCreatedAt,
              saved.clientSequence == message.clientSequence else {
          throw PersistenceError.messageConflict
        }
        return value
      }
      _ = try database.ensureConversation(first: message.senderId, second: message.receiverId)
      let record = MessageRecord(message: message, direction: .incoming, state: nil, receivedAt: receivedAt)
      database.context.insert(record)
      return try record.value()
    }
  }

  func markSending(id: UUID) throws {
    try updateOutgoing(id: id) { record in
      if record.stateRawValue == MessageState.pendingToSend.rawValue {
        record.stateRawValue = MessageState.sending.rawValue
      }
    }
  }

  func markAccepted(id: UUID, serverReceivedAt: Date) throws {
    try updateOutgoing(id: id) { record in
      // Late acceptance can settle a retry, but not rewrite a previously committed ACK timestamp.
      guard record.stateRawValue != MessageState.sent.rawValue else { return }
      record.stateRawValue = MessageState.sent.rawValue
      record.serverReceivedAt = serverReceivedAt
    }
  }

  func markRejected(id: UUID, retryable: Bool) throws {
    try updateOutgoing(id: id) { record in
      guard record.stateRawValue == MessageState.sending.rawValue
        || record.stateRawValue == MessageState.pendingToSend.rawValue else { return }
      record.stateRawValue = (retryable ? MessageState.pendingToSend : .failed).rawValue
    }
  }

  func recoverInterruptedSends() throws {
    try database.write {
      let sending = MessageState.sending.rawValue
      let outgoing = MessageDirection.outgoing.rawValue
      let records = try database.context.fetch(FetchDescriptor<MessageRecord>(
        predicate: #Predicate { $0.directionRawValue == outgoing && $0.stateRawValue == sending }
      ))
      for record in records { record.stateRawValue = MessageState.pendingToSend.rawValue }
    }
  }

  func observeMessages(conversationId: String) -> AsyncThrowingStream<[LocalMessage], any Error> {
    database.observe { [self] in try messages(conversationId: conversationId) }
  }

  private func updateOutgoing(id: UUID, update: (MessageRecord) -> Void) throws {
    try database.write {
      guard let record = try database.messageRecord(id) else { throw PersistenceError.messageNotFound }
      guard record.directionRawValue == MessageDirection.outgoing.rawValue else {
        throw PersistenceError.invalidData
      }
      update(record)
    }
  }
}
