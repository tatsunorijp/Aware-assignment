// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Messaging service with a real local server")
struct MessagingServiceIntegrationTests {
  @Test(.enabled(if: ProcessInfo.processInfo.environment["AWARE_SERVER_INTEGRATION_URL"] != nil))
  func twoIsolatedClientsPersistOfflineReplayAndLiveReply() async throws {
    let address = try #require(ProcessInfo.processInfo.environment["AWARE_SERVER_INTEGRATION_URL"])
    let baseURL = try #require(URL(string: address))
    var socketURL = try #require(URLComponents(url: baseURL.appendingPathComponent("ws"), resolvingAgainstBaseURL: false))
    socketURL.scheme = baseURL.scheme == "https" ? "wss" : "ws"
    let configuration = NetworkConfiguration(httpBaseURL: baseURL, webSocketURL: try #require(socketURL.url))
    let alice = try AppDependencies(
      database: PersistenceContainer(inMemory: true), apiClient: APIClient(configuration: configuration),
      webSocket: WebSocketClient(configuration: configuration)
    )
    let bob = try AppDependencies(
      database: PersistenceContainer(inMemory: true), apiClient: APIClient(configuration: configuration),
      webSocket: WebSocketClient(configuration: configuration)
    )
    let aliceUser = try alice.users.saveIdentity(name: "Integration Alice")
    let bobUser = try bob.users.saveIdentity(name: "Integration Bob")
    do {
      #expect(try await alice.apiClient.health().status == "ok")
      await bob.messaging.start()
      try await eventually { await bob.messaging.connectionState() == .connected }
      await bob.messaging.stop()

      let queued = try await alice.messaging.sendMessage(text: "Offline queue", receiverId: bobUser.userId)
      #expect(queued.state == .pendingToSend)
      await alice.messaging.start()
      try await eventually { try alice.messages.message(id: queued.id)?.state == .sent }
      #expect(try bob.messages.message(id: queued.id) == nil)
      let knownUsers = try await alice.apiClient.users()
      #expect(Set(knownUsers.map(\.userId)).isSuperset(of: [aliceUser.userId, bobUser.userId]))

      await bob.messaging.start()
      try await eventually { try bob.messages.message(id: queued.id) != nil }
      let received = try #require(try bob.messages.message(id: queued.id))
      #expect(received.direction == .incoming)
      #expect(received.state == nil)
      #expect(received.receivedAt != nil)
      #expect(received.message.serverReceivedAt != nil)
      #expect(received.message.text == "Offline queue")

      let reply = try await bob.messaging.sendMessage(text: "Live reply", receiverId: aliceUser.userId)
      try await eventually { try bob.messages.message(id: reply.id)?.state == .sent }
      try await eventually { try alice.messages.message(id: reply.id)?.direction == .incoming }
      #expect(try alice.conversations.conversations().count == 1)
      #expect(try bob.conversations.conversations().count == 1)
      #expect(try alice.users.currentUser()?.registrationCompleted == true)
      #expect(try bob.users.currentUser()?.registrationCompleted == true)
    } catch {
      await alice.messaging.stop()
      await bob.messaging.stop()
      throw error
    }
    await alice.messaging.stop()
    await bob.messaging.stop()
  }
}
// MARK: - AI Generated - End
