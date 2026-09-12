import Foundation
import Observation

@MainActor
@Observable
final class AppRouter {
  var path = [AppRoute]()

  func showMessages(with userId: UUID) {
    path.append(.messages(userId: userId))
  }

  func goBack() {
    guard !path.isEmpty else { return }
    path.removeLast()
  }

  func popToRoot() {
    path.removeAll()
  }
}
