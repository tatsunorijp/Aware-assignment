// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Network Manager")
struct NetworkManagerTests {
  private let request = URLRequest(url: URL(string: "http://example.test/health")!)

  @Test
  func fetchDecodesSuccessfulResponse() async throws {
    let transport = MockHTTPTransport { request in
      Self.response(
        statusCode: 200,
        data: Data(#"{"status":"ok","protocolVersion":1}"#.utf8),
        url: request.url!
      )
    }
    let manager = NetworkManager(transport: transport)

    let response = try await manager.fetch(request, as: HealthResponse.self)

    #expect(response.status == "ok")
    #expect(response.protocolVersion == 1)
  }

  @Test
  func structuredHTTPErrorIsPropagated() async throws {
    let body = try Self.fixtureData(named: "http_error_temporary.json")
    let transport = MockHTTPTransport { request in
      Self.response(statusCode: 503, data: body, url: request.url!)
    }
    let manager = NetworkManager(transport: transport)

    do {
      _ = try await manager.fetch(request, as: UsersResponse.self)
      Issue.record("Expected a structured server error")
    } catch let NetworkError.server(statusCode, error) {
      #expect(statusCode == 503)
      #expect(error.code == "TEMPORARY_UNAVAILABLE")
      #expect(error.isRetryable)
      #expect(error.requestId == "example-request-124")
    } catch {
      Issue.record("Unexpected error: \(error)")
    }
  }

  @Test
  func invalidHTTPErrorBodyIsNotFabricatedAsServerError() async {
    let transport = MockHTTPTransport { request in
      Self.response(statusCode: 500, data: Data("not-json".utf8), url: request.url!)
    }
    let manager = NetworkManager(transport: transport)

    await #expect(throws: NetworkError.invalidErrorResponse(statusCode: 500)) {
      _ = try await manager.fetch(request, as: HealthResponse.self)
    }
  }

  @Test
  func invalidSuccessBodyIsDecodingFailure() async {
    let transport = MockHTTPTransport { request in
      Self.response(
        statusCode: 200,
        data: Data(#"{"status":"wrong"}"#.utf8),
        url: request.url!
      )
    }
    let manager = NetworkManager(transport: transport)

    await #expect(throws: NetworkError.decoding) {
      _ = try await manager.fetch(request, as: HealthResponse.self)
    }
  }

  @Test
  func nonHTTPResponseIsRejected() async {
    let transport = MockHTTPTransport { request in
      (Data(), URLResponse(url: request.url!, mimeType: nil, expectedContentLength: 0, textEncodingName: nil))
    }
    let manager = NetworkManager(transport: transport)

    await #expect(throws: NetworkError.invalidHTTPResponse) {
      _ = try await manager.fetch(request, as: HealthResponse.self)
    }
  }

  @Test
  func transportErrorRemainsDistinct() async {
    let transport = MockHTTPTransport { _ in
      throw URLError(.notConnectedToInternet)
    }
    let manager = NetworkManager(transport: transport)

    await #expect(throws: NetworkError.transport(code: .notConnectedToInternet)) {
      _ = try await manager.fetch(request, as: UsersResponse.self)
    }
  }

  private static func response(
    statusCode: Int,
    data: Data,
    url: URL
  ) -> (Data, URLResponse) {
    (
      data,
      HTTPURLResponse(
        url: url,
        statusCode: statusCode,
        httpVersion: "HTTP/1.1",
        headerFields: ["Content-Type": "application/json"]
      )!
    )
  }

  private static func fixtureData(named name: String) throws -> Data {
    var url = URL(fileURLWithPath: #filePath)
    while url.lastPathComponent != "Aware-assignment", url.path != "/" {
      url.deleteLastPathComponent()
    }
    return try Data(contentsOf: url.appendingPathComponent("fixtures/protocol/\(name)"))
  }
}

private struct MockHTTPTransport: HTTPTransport {
  let handler: @Sendable (URLRequest) async throws -> (Data, URLResponse)

  init(handler: @escaping @Sendable (URLRequest) async throws -> (Data, URLResponse)) {
    self.handler = handler
  }

  func data(for request: URLRequest) async throws -> (Data, URLResponse) {
    try await handler(request)
  }
}
// MARK: - AI Generated - End
