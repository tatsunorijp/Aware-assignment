import Foundation
import SwiftData

@MainActor
final class SwiftDataUserLocalRepository: UserLocalRepository {
  private let database: PersistenceContainer

  init(database: PersistenceContainer) { self.database = database }

  func currentUser() throws -> LocalUser? {
    try database.read { try database.currentIdentityRecord()?.value }
  }

  func user(id: UUID) throws -> LocalUser? {
    try database.read { try database.userRecord(id)?.value }
  }

  @discardableResult
  func saveIdentity(name: String) throws -> LocalUser {
    try database.write {
      let name = try WireValidation.nonblank(name)
      if let record = try database.currentIdentityRecord() {
        // Reuse an incomplete registration's UUID; renaming a completed identity is not MVP scope.
        guard !record.registrationCompleted || record.name == name else {
          throw PersistenceError.identityConflict
        }
        record.name = name
        return record.value
      }
      let record = UserRecord(userId: UUID(), name: name)
      record.isCurrent = true
      database.context.insert(record)
      return record.value
    }
  }

  func completeRegistration(acceptedUser: UserDTO) throws {
    try database.write {
      guard let record = try database.currentIdentityRecord(),
            record.userId == acceptedUser.userId, record.name == acceptedUser.name else {
        throw PersistenceError.identityConflict
      }
      record.registrationCompleted = true
    }
  }

  func upsertKnownUsers(_ users: [UserDTO]) throws {
    try database.write {
      for user in users {
        if let record = try database.userRecord(user.userId) {
          // Discovery is not allowed to replace the locally owned identity/name/completion.
          if !record.isCurrent { record.name = user.name }
        } else {
          database.context.insert(UserRecord(userId: user.userId, name: user.name))
        }
      }
    }
  }

  func observeCurrentUser() -> AsyncThrowingStream<LocalUser?, any Error> {
    database.observe { [self] in try currentUser() }
  }
}
