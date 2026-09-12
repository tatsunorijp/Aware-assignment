// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Local conversations")
struct SwiftDataConversationLocalRepositoryTests {
  @Test func idempotentCreationAndSummaryFollowMessagesAndDiscovery() async throws {
    let store = try PersistenceTestStore()
    let user = try store.identify()
    let peer = UUID()
    var updates = store.conversations.observeConversations().makeAsyncIterator()
    #expect(try await updates.next() == [])
    let first = try store.conversations.getOrCreate(with: peer)
    #expect(try store.conversations.getOrCreate(with: peer) == first)
    #expect(first.conversationId == (try WireValidation.conversationID(first: peer, second: user.userId)))
    #expect(try store.conversations.conversations().count == 1)
    #expect(first.peer.name == nil)
    let message = try store.messages.enqueue(text: "Offline", receiverId: peer, createdAt: .now)
    #expect(try await updates.next()?.first?.latestMessage == message)
    try store.messages.markAccepted(id: message.id, serverReceivedAt: .now)
    #expect(try await updates.next()?.first?.latestMessage?.state == .sent)
    try store.users.upsertKnownUsers([try UserDTO(userId: peer, name: "Bob")])
    #expect(try await updates.next()?.first?.peer.name == "Bob")
  }
}
// MARK: - AI Generated - End
