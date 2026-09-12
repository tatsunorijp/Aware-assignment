// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("App Route")
struct AppRouteTests {
  @Test
  func messagesRouteCarriesOnlyTheDestinationIdentifier() throws {
    let userId = try #require(
      UUID(uuidString: "11111111-1111-4111-8111-111111111111")
    )

    let route = AppRoute.messages(userId: userId)

    #expect(route == .messages(userId: userId))
  }

  @Test
  func equivalentRoutesHaveTheSameHashIdentity() throws {
    let userId = try #require(
      UUID(uuidString: "22222222-2222-4222-8222-222222222222")
    )

    let routes = Set([
      AppRoute.messages(userId: userId),
      AppRoute.messages(userId: userId),
    ])

    #expect(routes.count == 1)
  }
}
// MARK: - AI Generated - End
