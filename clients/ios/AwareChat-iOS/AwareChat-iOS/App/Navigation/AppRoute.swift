import Foundation

nonisolated enum AppRoute: Hashable, Sendable {
  case messages(userId: UUID)
}
