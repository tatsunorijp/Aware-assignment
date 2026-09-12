import Foundation
import SwiftData

@Model
final class ConversationRecord {
  @Attribute(.unique) var conversationId: String
  var firstParticipantId: UUID
  var secondParticipantId: UUID

  init(conversationId: String, first: UUID, second: UUID) {
    self.conversationId = conversationId
    firstParticipantId = first
    secondParticipantId = second
  }
}
