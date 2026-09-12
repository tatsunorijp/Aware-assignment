// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Persistence durability")
struct PersistenceContainerTests {
  @Test func diskReopenPreservesIdentityMessagesReceiptAndSequence() throws {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    defer { try? FileManager.default.removeItem(at: directory) }
    let url = directory.appendingPathComponent("test.store")
    let peer = UUID()
    let receipt = Date(timeIntervalSince1970: 400)

    func seed() throws -> (UserDTO, UUID, UUID) {
      let store = try PersistenceTestStore(url: url)
      let user = try store.identify()
      try store.users.completeRegistration(acceptedUser: user)
      let outgoing = try store.messages.enqueue(text: "Durable", receiverId: peer, createdAt: .now)
      try store.messages.markSending(id: outgoing.id)
      let incoming = try store.incoming(from: peer)
      try store.messages.persistIncoming(incoming, receivedAt: receipt)
      return (user, outgoing.id, incoming.messageId)
    }

    let (identity, outgoingID, incomingID) = try seed()
    let reopened = try PersistenceTestStore(url: url)
    #expect(try reopened.users.currentUser()?.wireIdentity() == identity)
    #expect(try reopened.users.currentUser()?.registrationCompleted == true)
    #expect(try reopened.messages.message(id: outgoingID)?.state == .sending)
    try reopened.messages.recoverInterruptedSends()
    #expect(try reopened.messages.pendingMessages().map(\.id) == [outgoingID])
    #expect(try reopened.messages.message(id: incomingID)?.receivedAt == receipt)
    let next = try reopened.messages.enqueue(text: "Next", receiverId: peer, createdAt: .now)
    #expect(next.message.clientSequence == 2)
  }
}
// MARK: - AI Generated - End
