import Foundation

/// Retain one instance at the app root; inject only the contracts each consumer needs.
@MainActor
final class AppDependencies {
  let users: any UserLocalRepository
  let conversations: any ConversationLocalRepository
  let messages: any MessageLocalRepository
  let messaging: any MessagingServiceProtocol
  let apiClient: any APIClientProtocol

  init(
    database: PersistenceContainer,
    apiClient: any APIClientProtocol,
    webSocket: any WebSocketClientProtocol,
    clock: any MessagingClock = SystemMessagingClock()
  ) {
    let users = SwiftDataUserLocalRepository(database: database)
    let messages = SwiftDataMessageLocalRepository(database: database)
    self.users = users
    self.messages = messages
    conversations = SwiftDataConversationLocalRepository(database: database)
    self.apiClient = apiClient
    messaging = MessagingService(socket: webSocket, users: users, messages: messages, clock: clock)
  }

  static func live(configuration: NetworkConfiguration = .localDevelopment) throws -> AppDependencies {
    try AppDependencies(
      database: PersistenceContainer(),
      apiClient: APIClient(configuration: configuration),
      webSocket: WebSocketClient(configuration: configuration)
    )
  }
}
