// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("WebSocket Transport")
struct WebSocketTransportTests {
  @Test
  func frameAndCloseDetailsPreserveTransportValues() {
    #expect(WebSocketFrame.text("hello") == .text("hello"))
    #expect(WebSocketFrame.binary(Data([0x01])) == .binary(Data([0x01])))
    #expect(
      WebSocketCloseDetails(code: 4001, reason: "Session replaced")
        == WebSocketCloseDetails(code: 4001, reason: "Session replaced")
    )
  }

  @Test
  func factoryCreatesInactiveTransportForConfiguredURL() {
    let session = URLSession(configuration: .ephemeral)
    let factory = URLSessionWebSocketTransportFactory(session: session)
    let transport = factory.makeTransport(url: URL(string: "ws://example.invalid/ws")!)

    #expect(transport.closeDetails == nil)
    transport.cancel(code: .normalClosure, reason: nil)
    session.invalidateAndCancel()
  }
}
// MARK: - AI Generated - End
