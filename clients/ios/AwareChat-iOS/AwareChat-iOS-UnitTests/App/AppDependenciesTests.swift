// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("App dependency composition")
struct AppDependenciesTests {
  @Test func repositoriesAndServiceShareTheInjectedDatabase() async throws {
    let store = try PersistenceTestStore()
    let socket = TestMessagingSocket()
    let dependencies = AppDependencies(
      database: store.database,
      apiClient: APIClient(configuration: .localDevelopment),
      webSocket: socket,
      clock: TestMessagingClock()
    )
    let user = try dependencies.users.saveIdentity(name: "Alice")
    let message = try await dependencies.messaging.sendMessage(text: "Offline", receiverId: UUID())
    #expect(try store.users.currentUser() == user)
    #expect(try dependencies.messages.message(id: message.id) == message)
    #expect(try dependencies.conversations.conversations().first?.latestMessage == message)
    #expect(await socket.connections == 0)
  }
}
// MARK: - AI Generated - End
