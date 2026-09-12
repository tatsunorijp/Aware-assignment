// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Network Configuration")
struct NetworkConfigurationTests {
  @Test
  func localDevelopmentUsesDocumentedLoopbackAddresses() {
    #expect(
      NetworkConfiguration.localDevelopment.httpBaseURL.absoluteString
        == "http://127.0.0.1:8000"
    )
    #expect(
      NetworkConfiguration.localDevelopment.webSocketURL.absoluteString
        == "ws://127.0.0.1:8000/ws"
    )
  }

  @Test
  func configurationAcceptsInjectedAddresses() {
    let configuration = NetworkConfiguration(
      httpBaseURL: URL(string: "http://192.0.2.1:9000")!,
      webSocketURL: URL(string: "ws://192.0.2.1:9000/socket")!
    )

    #expect(configuration.httpBaseURL.host == "192.0.2.1")
    #expect(configuration.webSocketURL.path == "/socket")
  }
}
// MARK: - AI Generated - End
