// MARK: - AI Generated - Start
import Foundation
import Testing

/// Real time only bounds a failed test; protocol deadlines use the injected test clock.
@MainActor
func eventually(_ condition: @escaping @MainActor () async throws -> Bool) async throws {
  let deadline = ContinuousClock.now.advanced(by: .seconds(8))
  while try await !condition() {
    try #require(ContinuousClock.now < deadline, "The expected asynchronous condition was not reached.")
    try await Task.sleep(for: .milliseconds(2))
  }
}
// MARK: - AI Generated - End
