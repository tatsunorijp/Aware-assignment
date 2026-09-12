import Foundation

nonisolated enum APIEndpoint: String, CaseIterable, Sendable {
  case health
  case users

  func request(configuration: NetworkConfiguration) -> URLRequest {
    var request = URLRequest(
      url: configuration.httpBaseURL.appendingPathComponent(rawValue)
    )
    request.httpMethod = "GET"
    request.setValue("application/json", forHTTPHeaderField: "Accept")
    return request
  }
}
