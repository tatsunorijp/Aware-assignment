import Foundation
import SwiftData

@MainActor
final class SwiftDataConversationLocalRepository: ConversationLocalRepository {
  private let database: PersistenceContainer

  init(database: PersistenceContainer) { self.database = database }

  func conversations() throws -> [LocalConversation] {
    try database.read {
      try database.context.fetch(FetchDescriptor<ConversationRecord>()).map(value).sorted {
        let firstDate = $0.latestMessage?.displayTimestamp ?? .distantPast
        let secondDate = $1.latestMessage?.displayTimestamp ?? .distantPast
        return firstDate == secondDate ? $0.id < $1.id : firstDate > secondDate
      }
    }
  }

  func conversation(id: String) throws -> LocalConversation? {
    try database.read { try database.conversationRecord(id).map(value) }
  }

  @discardableResult
  func getOrCreate(with userId: UUID) throws -> LocalConversation {
    try database.write {
      guard let current = try database.currentIdentityRecord() else {
        throw PersistenceError.missingIdentity
      }
      return try value(database.ensureConversation(first: current.userId, second: userId))
    }
  }

  func observeConversations() -> AsyncThrowingStream<[LocalConversation], any Error> {
    database.observe { [self] in try conversations() }
  }

  private func value(_ record: ConversationRecord) throws -> LocalConversation {
    guard let current = try database.currentIdentityRecord() else {
      throw PersistenceError.missingIdentity
    }
    guard record.firstParticipantId == current.userId || record.secondParticipantId == current.userId else {
      throw PersistenceError.invalidData
    }
    let peerId = record.firstParticipantId == current.userId
      ? record.secondParticipantId : record.firstParticipantId
    guard let peer = try database.userRecord(peerId) else { throw PersistenceError.invalidData }
    let messages = try database.messageRecords(conversationId: record.conversationId)
    return try LocalConversation(conversationId: record.conversationId, peer: peer.value,
                                 latestMessage: messages.last?.value())
  }
}
