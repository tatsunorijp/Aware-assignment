// MARK: - AI Generated - Start
import Testing
@testable import AwareChat_iOS

@Suite("App Flow")
struct AppFlowTests {
  @Test("Flows remain distinct typed states")
  func flowsAreDistinct() {
    #expect(AppFlow.loading != .identification)
    #expect(AppFlow.loading != .conversations)
    #expect(AppFlow.identification != .conversations)
  }
}
// MARK: - AI Generated - End
