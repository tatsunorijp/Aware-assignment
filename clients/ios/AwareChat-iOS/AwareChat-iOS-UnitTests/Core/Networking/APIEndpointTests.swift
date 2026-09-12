// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("API Endpoint")
struct APIEndpointTests {
  private let configuration = NetworkConfiguration(
    httpBaseURL: URL(string: "http://example.test:8080/api")!,
    webSocketURL: URL(string: "ws://example.test:8080/ws")!
  )

  @Test
  func healthRequest() {
    let request = APIEndpoint.health.request(configuration: configuration)

    #expect(request.url?.absoluteString == "http://example.test:8080/api/health")
    #expect(request.httpMethod == "GET")
    #expect(request.value(forHTTPHeaderField: "Accept") == "application/json")
    #expect(request.httpBody == nil)
  }

  @Test
  func usersRequest() {
    let request = APIEndpoint.users.request(configuration: configuration)

    #expect(request.url?.absoluteString == "http://example.test:8080/api/users")
    #expect(request.httpMethod == "GET")
  }

  @Test
  func allServerHTTPEndpointsAreRepresented() {
    #expect(Set(APIEndpoint.allCases) == [.health, .users])
  }
}
// MARK: - AI Generated - End
