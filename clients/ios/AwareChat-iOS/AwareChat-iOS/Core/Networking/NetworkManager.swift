import Foundation

nonisolated protocol HTTPTransport: Sendable {
  func data(for request: URLRequest) async throws -> (Data, URLResponse)
}

extension URLSession: HTTPTransport {}

nonisolated protocol NetworkProtocol: Sendable {
  func fetch<Response: Decodable & Sendable>(
    _ request: URLRequest,
    as type: Response.Type
  ) async throws -> Response
}

nonisolated extension NetworkProtocol {
  func fetch<Response: Decodable & Sendable>(
    _ endpoint: APIEndpoint,
    configuration: NetworkConfiguration,
    as type: Response.Type
  ) async throws -> Response {
    try await fetch(endpoint.request(configuration: configuration), as: type)
  }
}

nonisolated final class NetworkManager: NetworkProtocol, Sendable {
  private let transport: any HTTPTransport

  init(transport: any HTTPTransport = URLSession.shared) {
    self.transport = transport
  }

  func fetch<Response: Decodable & Sendable>(
    _ request: URLRequest,
    as type: Response.Type
  ) async throws -> Response {
    let data: Data
    let response: URLResponse

    do {
      (data, response) = try await transport.data(for: request)
    } catch {
      throw NetworkError.wrapping(error)
    }

    guard let httpResponse = response as? HTTPURLResponse else {
      throw NetworkError.invalidHTTPResponse
    }

    guard (200...299).contains(httpResponse.statusCode) else {
      guard let envelope = try? JSONDecoder().decode(HTTPErrorEnvelope.self, from: data) else {
        throw NetworkError.invalidErrorResponse(statusCode: httpResponse.statusCode)
      }
      throw NetworkError.server(statusCode: httpResponse.statusCode, error: envelope.error)
    }

    do {
      return try JSONDecoder().decode(type, from: data)
    } catch {
      throw NetworkError.decoding
    }
  }
}
