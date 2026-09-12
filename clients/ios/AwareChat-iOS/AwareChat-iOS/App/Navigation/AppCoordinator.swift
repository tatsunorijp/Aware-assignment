import Observation

@MainActor
@Observable
final class AppCoordinator {
  private(set) var flow = AppFlow.loading
  let router: AppRouter

  init(router: AppRouter) {
    self.router = router
  }

  convenience init() {
    self.init(router: AppRouter())
  }

  func start(hasCompletedRegistration: Bool) {
    router.popToRoot()
    flow = hasCompletedRegistration ? .conversations : .identification
  }

  func showLoading() {
    router.popToRoot()
    flow = .loading
  }

  func didCompleteRegistration() {
    router.popToRoot()
    flow = .conversations
  }
}
