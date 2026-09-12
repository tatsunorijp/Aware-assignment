import Foundation

nonisolated struct NetworkConfiguration: Equatable, Sendable {
  let httpBaseURL: URL
  let webSocketURL: URL

  init(httpBaseURL: URL, webSocketURL: URL) {
    self.httpBaseURL = httpBaseURL
    self.webSocketURL = webSocketURL
  }

  static let localDevelopment = NetworkConfiguration(
    httpBaseURL: URL(string: "http://127.0.0.1:8000")!,
    webSocketURL: URL(string: "ws://127.0.0.1:8000/ws")!
  )
}
