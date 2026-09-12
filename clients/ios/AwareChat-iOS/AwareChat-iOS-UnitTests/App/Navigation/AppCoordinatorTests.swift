// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("App Coordinator")
struct AppCoordinatorTests {
  @Test
  func coordinatorStartsInLoadingFlow() {
    let coordinator = AppCoordinator()

    #expect(coordinator.flow == .loading)
    #expect(coordinator.router.path.isEmpty)
  }

  @Test(arguments: [false, true])
  func startSelectsThePersistedRegistrationFlow(
    hasCompletedRegistration: Bool
  ) throws {
    let router = AppRouter()
    let userId = try #require(
      UUID(uuidString: "11111111-1111-4111-8111-111111111111")
    )
    router.path = [.messages(userId: userId)]
    let coordinator = AppCoordinator(router: router)

    coordinator.start(hasCompletedRegistration: hasCompletedRegistration)

    let expectedFlow: AppFlow = hasCompletedRegistration
      ? .conversations
      : .identification
    #expect(coordinator.flow == expectedFlow)
    #expect(router.path.isEmpty)
  }

  @Test
  func completedRegistrationSelectsConversationsAndResetsRoutes() throws {
    let router = AppRouter()
    let userId = try #require(
      UUID(uuidString: "22222222-2222-4222-8222-222222222222")
    )
    router.path = [.messages(userId: userId)]
    let coordinator = AppCoordinator(router: router)

    coordinator.didCompleteRegistration()

    #expect(coordinator.flow == .conversations)
    #expect(router.path.isEmpty)
  }

  @Test
  func showLoadingSelectsLoadingAndResetsRoutes() throws {
    let router = AppRouter()
    let userId = try #require(
      UUID(uuidString: "22222222-2222-4222-8222-222222222222")
    )
    router.path = [.messages(userId: userId)]
    let coordinator = AppCoordinator(router: router)

    coordinator.showLoading()

    #expect(coordinator.flow == .loading)
    #expect(router.path.isEmpty)
  }
}
// MARK: - AI Generated - End
