import Foundation

protocol MessagingClock: Sendable {
  func now() -> Date
  func sleep(for duration: Duration) async throws
}

nonisolated struct SystemMessagingClock: MessagingClock {
  func now() -> Date { Date() }
  func sleep(for duration: Duration) async throws { try await Task.sleep(for: duration) }
}

nonisolated struct MessagingRetryPolicy: Sendable {
  static let acceptanceTimeout: Duration = .seconds(10)
  static let identificationTimeout: Duration = .seconds(10)
  private var index = 0
  private static let delays: [Duration] = [1, 2, 4, 8, 16, 30].map { .seconds($0) }

  mutating func nextDelay() -> Duration {
    let delay = Self.delays[index]
    index = min(index + 1, Self.delays.count - 1)
    return delay
  }

  mutating func reset() { index = 0 }
}
