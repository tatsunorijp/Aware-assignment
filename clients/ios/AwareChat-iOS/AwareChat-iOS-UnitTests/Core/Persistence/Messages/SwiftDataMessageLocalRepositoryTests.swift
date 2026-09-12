// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Local messages")
struct SwiftDataMessageLocalRepositoryTests {
  @Test func concurrentEnqueuesUseUniqueDurableFIFOSequenceNotClockOrder() async throws {
    let store = try PersistenceTestStore()
    _ = try store.identify()
    let repository = store.messages
    let peer = UUID()
    try await withThrowingTaskGroup(of: Void.self) { group in
      for index in 0..<20 {
        group.addTask {
          try await repository.enqueue(text: "Message \(index)", receiverId: peer,
                                       createdAt: Date(timeIntervalSince1970: Double(100 - index)))
        }
      }
      try await group.waitForAll()
    }
    let pending = try repository.pendingMessages()
    #expect(pending.map(\.message.clientSequence) == Array(Int64(1)...20))
    #expect(Set(pending.map(\.id)).count == 20)
    #expect(try store.conversations.conversations().count == 1)
  }

  @Test func acceptedMessagesNeverDowngradeAndRetriesKeepPayload() throws {
    let store = try PersistenceTestStore()
    _ = try store.identify()
    let original = try store.messages.enqueue(text: " Hello ", receiverId: UUID(), createdAt: .now)
    try store.messages.markSending(id: original.id)
    try store.messages.recoverInterruptedSends()
    #expect(try store.messages.pendingMessages().first?.message == original.message)
    try store.messages.markSending(id: original.id)
    let timestamp = Date(timeIntervalSince1970: 500)
    try store.messages.markAccepted(id: original.id, serverReceivedAt: timestamp)
    try store.messages.markRejected(id: original.id, retryable: false)
    try store.messages.markRejected(id: original.id, retryable: true)
    try store.messages.markAccepted(id: original.id, serverReceivedAt: timestamp.addingTimeInterval(1))
    try store.messages.recoverInterruptedSends()
    let saved = try #require(try store.messages.message(id: original.id))
    #expect(saved.state == .sent)
    #expect(saved.message.serverReceivedAt == timestamp)
    #expect(try saved.outgoingPayload() == original.message)
    #expect(try store.messages.pendingMessages().isEmpty)
  }

  @Test func incomingDuplicatesKeepReceiptTimeAndCreateRelatedRecords() throws {
    let store = try PersistenceTestStore()
    let incoming = try store.incoming()
    let first = try store.messages.persistIncoming(incoming, receivedAt: Date(timeIntervalSince1970: 200))
    let duplicate = try store.messages.persistIncoming(incoming, receivedAt: Date(timeIntervalSince1970: 300))
    #expect(first == duplicate)
    #expect(first.state == nil)
    #expect(first.displayTimestamp == Date(timeIntervalSince1970: 200))
    #expect(try store.messages.messages(conversationId: incoming.conversationId).count == 1)
    #expect(try store.users.user(id: incoming.senderId)?.name == nil)
    #expect(try store.conversations.conversation(id: incoming.conversationId)?.latestMessage == first)
    let replayAfterRestart = try MessageDTO(
      messageId: incoming.messageId, text: incoming.text, senderId: incoming.senderId,
      receiverId: incoming.receiverId, clientCreatedAt: incoming.clientCreatedAt,
      clientSequence: incoming.clientSequence, serverReceivedAt: Date(timeIntervalSince1970: 999)
    )
    #expect(try store.messages.persistIncoming(replayAfterRestart, receivedAt: .now) == first)
    #expect(throws: PersistenceError.invalidData) { try store.messages.markSending(id: incoming.messageId) }
    let conflict = try MessageDTO(
      messageId: incoming.messageId, text: "Different content", senderId: incoming.senderId,
      receiverId: incoming.receiverId, clientCreatedAt: incoming.clientCreatedAt,
      clientSequence: 1, serverReceivedAt: incoming.serverReceivedAt
    )
    #expect(throws: PersistenceError.messageConflict) {
      try store.messages.persistIncoming(conflict, receivedAt: .now)
    }
  }

  @Test func failedOutgoingWriteRollsBackConversationSequenceAndObservation() async throws {
    let store = try PersistenceTestStore()
    let user = try store.identify()
    let peer = UUID()
    let conversation = try WireValidation.conversationID(first: user.userId, second: peer)
    var updates = store.messages.observeMessages(conversationId: conversation).makeAsyncIterator()
    #expect(try await updates.next() == [])
    store.writes.failNext = true
    #expect(throws: PersistenceError.writeFailed) {
      try store.messages.enqueue(text: "Unsaved", receiverId: peer, createdAt: .now)
    }
    #expect(try store.conversations.conversations().isEmpty)
    #expect(try store.users.user(id: peer) == nil)
    #expect(try store.messages.pendingMessages().isEmpty)
    let saved = try store.messages.enqueue(text: "Saved", receiverId: peer, createdAt: .now)
    #expect(saved.message.clientSequence == 1)
    #expect(try await updates.next() == [saved])
    #expect(throws: PersistenceError.invalidData) {
      try store.messages.enqueue(text: " \n ", receiverId: peer, createdAt: .now)
    }
  }

  @Test func failedIncomingWriteLeavesNoPartialRecordsForLaterSave() throws {
    let store = try PersistenceTestStore()
    let incoming = try store.incoming()
    store.writes.failNext = true
    #expect(throws: PersistenceError.writeFailed) {
      try store.messages.persistIncoming(incoming, receivedAt: .now)
    }
    try store.users.upsertKnownUsers([try UserDTO(userId: UUID(), name: "Unrelated")])
    #expect(try store.messages.message(id: incoming.messageId) == nil)
    #expect(try store.users.user(id: incoming.senderId) == nil)
    #expect(try store.conversations.conversations().isEmpty)
  }
}
// MARK: - AI Generated - End
