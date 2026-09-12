// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("App Router")
struct AppRouterTests {
  @Test
  func showMessagesAppendsATypedRoute() throws {
    let router = AppRouter()
    let userId = try #require(
      UUID(uuidString: "11111111-1111-4111-8111-111111111111")
    )

    router.showMessages(with: userId)

    #expect(router.path == [.messages(userId: userId)])
  }

  @Test
  func goBackRemovesOnlyTheLastRoute() throws {
    let router = AppRouter()
    let aliceId = try #require(
      UUID(uuidString: "11111111-1111-4111-8111-111111111111")
    )
    let bobId = try #require(
      UUID(uuidString: "22222222-2222-4222-8222-222222222222")
    )
    router.path = [
      .messages(userId: aliceId),
      .messages(userId: bobId),
    ]

    router.goBack()

    #expect(router.path == [.messages(userId: aliceId)])
  }

  @Test
  func goBackIsSafeAtTheRoot() {
    let router = AppRouter()

    router.goBack()

    #expect(router.path.isEmpty)
  }

  @Test
  func popToRootClearsThePath() throws {
    let router = AppRouter()
    let userId = try #require(
      UUID(uuidString: "11111111-1111-4111-8111-111111111111")
    )
    router.path = [.messages(userId: userId)]

    router.popToRoot()

    #expect(router.path.isEmpty)
  }
}
// MARK: - AI Generated - End
