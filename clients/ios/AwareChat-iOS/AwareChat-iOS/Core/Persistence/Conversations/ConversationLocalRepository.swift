import Foundation

nonisolated struct LocalConversation: Equatable, Sendable, Identifiable {
  let conversationId: String
  let peer: LocalUser
  let latestMessage: LocalMessage?
  var id: String { conversationId }
}

@MainActor
protocol ConversationLocalRepository: Sendable {
  func conversations() throws -> [LocalConversation]
  func conversation(id: String) throws -> LocalConversation?
  @discardableResult func getOrCreate(with userId: UUID) throws -> LocalConversation
  func observeConversations() -> AsyncThrowingStream<[LocalConversation], any Error>
}
