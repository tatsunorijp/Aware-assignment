import Foundation
import SwiftData

@Model
final class UserRecord {
  @Attribute(.unique) var userId: UUID
  var name: String?
  var isCurrent: Bool
  var registrationCompleted: Bool
  var lastClientSequence: Int64

  init(userId: UUID, name: String?) {
    self.userId = userId
    self.name = name
    isCurrent = false
    registrationCompleted = false
    lastClientSequence = 0
  }

  var value: LocalUser {
    LocalUser(userId: userId, name: name, isCurrent: isCurrent,
              registrationCompleted: registrationCompleted)
  }
}
