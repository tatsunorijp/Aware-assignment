// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("API Client")
struct APIClientTests {
  private let configuration = NetworkConfiguration(
    httpBaseURL: URL(string: "http://example.test")!,
    webSocketURL: URL(string: "ws://example.test/ws")!
  )

  @Test
  func healthUsesTypedEndpointAndReturnsDomainResponse() async throws {
    let networkManager = StubNetworkManager(
      expectedPath: "/health",
      result: .success(Data(#"{"status":"ok","protocolVersion":1}"#.utf8))
    )
    let client = APIClient(
      configuration: configuration,
      networkManager: networkManager
    )

    let response = try await client.health()
    let expectedResponse = try HealthResponse.fixture

    #expect(response == expectedResponse)
  }

  @Test
  func usersUnwrapsTheTransportEnvelope() async throws {
    let data = Data(
      #"{"users":[{"userId":"11111111-1111-4111-8111-111111111111","name":"Alice","future":true}],"futureEnvelope":1}"#.utf8
    )
    let networkManager = StubNetworkManager(
      expectedPath: "/users",
      result: .success(data)
    )
    let client = APIClient(
      configuration: configuration,
      networkManager: networkManager
    )

    let users = try await client.users()

    #expect(users.count == 1)
    #expect(users.first?.name == "Alice")
    #expect(
      users.first?.userId.uuidString.lowercased()
        == "11111111-1111-4111-8111-111111111111"
    )
  }

  @Test
  func networkFailureIsPropagatedWithoutRemapping() async {
    let networkManager = StubNetworkManager(
      expectedPath: "/users",
      result: .failure(.transport(code: .timedOut))
    )
    let client = APIClient(
      configuration: configuration,
      networkManager: networkManager
    )

    await #expect(throws: NetworkError.transport(code: .timedOut)) {
      _ = try await client.users()
    }
  }
}

private struct StubNetworkManager: NetworkProtocol {
  let expectedPath: String
  let result: Result<Data, NetworkError>

  func fetch<Response: Decodable & Sendable>(
    _ request: URLRequest,
    as type: Response.Type
  ) async throws -> Response {
    guard request.url?.path == expectedPath else {
      throw StubNetworkError.unexpectedPath
    }
    return try JSONDecoder().decode(type, from: result.get())
  }
}

private enum StubNetworkError: Error {
  case unexpectedPath
}

private extension HealthResponse {
  static var fixture: HealthResponse {
    get throws {
      try JSONDecoder().decode(
        HealthResponse.self,
        from: Data(#"{"status":"ok","protocolVersion":1}"#.utf8)
      )
    }
  }
}
// MARK: - AI Generated - End
