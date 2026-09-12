import Foundation

nonisolated struct LocalUser: Equatable, Sendable, Identifiable {
  let userId: UUID
  /// Nil means a received message introduced this user before discovery loaded their name.
  let name: String?
  let isCurrent: Bool
  let registrationCompleted: Bool
  var id: UUID { userId }

  func wireIdentity() throws -> UserDTO {
    guard isCurrent, let name else { throw PersistenceError.missingIdentity }
    return try UserDTO(userId: userId, name: name)
  }
}

@MainActor
protocol UserLocalRepository: Sendable {
  func currentUser() throws -> LocalUser?
  func user(id: UUID) throws -> LocalUser?
  @discardableResult func saveIdentity(name: String) throws -> LocalUser
  func completeRegistration(acceptedUser: UserDTO) throws
  func upsertKnownUsers(_ users: [UserDTO]) throws
  func observeCurrentUser() -> AsyncThrowingStream<LocalUser?, any Error>
}
