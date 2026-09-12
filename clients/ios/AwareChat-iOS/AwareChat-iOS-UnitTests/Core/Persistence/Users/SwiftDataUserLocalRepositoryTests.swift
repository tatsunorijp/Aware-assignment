// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Local users")
struct SwiftDataUserLocalRepositoryTests {
  @Test func identityRetryReusesIDAndDiscoveryPreservesCompletion() throws {
    let store = try PersistenceTestStore()
    let peer = try UserDTO(userId: UUID(), name: "Bob")
    try store.users.upsertKnownUsers([peer])
    #expect(try store.users.currentUser() == nil)
    let first = try store.identify()
    #expect(try store.users.currentUser()?.registrationCompleted == false)
    let retry = try store.users.saveIdentity(name: "Alice updated")
    #expect(first.userId == retry.userId)
    #expect(throws: PersistenceError.identityConflict) {
      try store.users.completeRegistration(acceptedUser: first)
    }
    let accepted = try retry.wireIdentity()
    try store.users.completeRegistration(acceptedUser: accepted)
    try store.users.upsertKnownUsers([try UserDTO(userId: retry.userId, name: "Stale discovery")])
    #expect(try store.users.currentUser()?.registrationCompleted == true)
    #expect(try store.users.currentUser()?.name == "Alice updated")
    #expect(throws: PersistenceError.identityConflict) { try store.users.saveIdentity(name: "Rename") }
  }

  @Test func observationStartsWithLocalValueAndCompletionRequiresSave() async throws {
    let store = try PersistenceTestStore()
    var updates = store.users.observeCurrentUser().makeAsyncIterator()
    let initial = try await updates.next()
    #expect(initial != nil)
    #expect(initial! == nil)
    let user = try store.identify()
    #expect(try await updates.next()??.userId == user.userId)
    store.writes.failNext = true
    #expect(throws: PersistenceError.writeFailed) { try store.users.completeRegistration(acceptedUser: user) }
    #expect(try store.users.currentUser()?.registrationCompleted == false)
    try store.users.completeRegistration(acceptedUser: user)
    #expect(try await updates.next()??.registrationCompleted == true)
  }
}
// MARK: - AI Generated - End
