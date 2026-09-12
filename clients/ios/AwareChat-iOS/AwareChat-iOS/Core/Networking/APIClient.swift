import Foundation

nonisolated protocol APIClientProtocol: Sendable {
  func health() async throws -> HealthResponse
  func users() async throws -> [UserDTO]
}

nonisolated final class APIClient: APIClientProtocol, Sendable {
  private let configuration: NetworkConfiguration
  private let networkManager: any NetworkProtocol

  init(
    configuration: NetworkConfiguration,
    networkManager: any NetworkProtocol = NetworkManager()
  ) {
    self.configuration = configuration
    self.networkManager = networkManager
  }

  func health() async throws -> HealthResponse {
    try await networkManager.fetch(
      .health,
      configuration: configuration,
      as: HealthResponse.self
    )
  }

  func users() async throws -> [UserDTO] {
    let response = try await networkManager.fetch(
      .users,
      configuration: configuration,
      as: UsersResponse.self
    )
    return response.users
  }
}
