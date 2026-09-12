// MARK: - AI Generated - Start
import Testing
@testable import AwareChat_iOS

@Suite("Messaging retry policy")
struct MessagingClockTests {
  @Test func backoffIsCappedAndResetsOnlyWhenRequested() {
    var policy = MessagingRetryPolicy()
    #expect((0..<8).map { _ in policy.nextDelay() } == [1, 2, 4, 8, 16, 30, 30, 30].map { .seconds($0) })
    policy.reset()
    #expect(policy.nextDelay() == .seconds(1))
    #expect(MessagingRetryPolicy.acceptanceTimeout == .seconds(10))
  }
}
// MARK: - AI Generated - End
