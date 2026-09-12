// MARK: - AI Generated - Start
import Foundation
import SwiftData
@testable import AwareChat_iOS

@MainActor
final class PersistenceTestStore {
  let database: PersistenceContainer
  let users: SwiftDataUserLocalRepository
  let conversations: SwiftDataConversationLocalRepository
  let messages: SwiftDataMessageLocalRepository
  let writes: TestSaveControl

  init(url: URL? = nil) throws {
    let writes = TestSaveControl()
    self.writes = writes
    database = try PersistenceContainer(inMemory: url == nil, storeURL: url) { context in
      if writes.failNext {
        writes.failNext = false
        throw PersistenceError.writeFailed
      }
      try context.save()
      writes.successfulSaves += 1
    }
    users = SwiftDataUserLocalRepository(database: database)
    conversations = SwiftDataConversationLocalRepository(database: database)
    messages = SwiftDataMessageLocalRepository(database: database)
  }

  func identify() throws -> UserDTO {
    try users.saveIdentity(name: "Alice").wireIdentity()
  }

  func incoming(from peer: UUID = UUID(), id: UUID = UUID()) throws -> MessageDTO {
    let user = try identify()
    return try MessageDTO(
      messageId: id, text: "Hello", senderId: peer, receiverId: user.userId,
      clientCreatedAt: Date(timeIntervalSince1970: 100), clientSequence: 1,
      serverReceivedAt: Date(timeIntervalSince1970: 101)
    )
  }
}

@MainActor
final class TestSaveControl {
  var failNext = false
  var successfulSaves = 0
}
// MARK: - AI Generated - End
