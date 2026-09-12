// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Identification ViewModel")
struct IdentificationViewModelTests {
  @Test
  func missingIdentityShowsAnEmptyForm() async {
    let harness = IdentificationHarness()

    await harness.viewModel.load()

    #expect(harness.viewModel.state == .form)
    #expect(harness.viewModel.name.isEmpty)
    #expect(harness.completions == 0)
  }

  @Test
  func incompleteIdentityPrefillsTheForm() async {
    let identity = LocalUser(
      userId: IdentificationFakeUserRepository.fixedID,
      name: "Alice",
      isCurrent: true,
      registrationCompleted: false
    )
    let harness = IdentificationHarness(currentUser: identity)

    await harness.viewModel.load()

    #expect(harness.viewModel.state == .form)
    #expect(harness.viewModel.name == "Alice")
    #expect(await harness.messaging.startCount == 0)
  }

  @Test
  func completedIdentitySkipsTheFormAndStartsBackgroundMessaging() async {
    let identity = LocalUser(
      userId: IdentificationFakeUserRepository.fixedID,
      name: "Alice",
      isCurrent: true,
      registrationCompleted: true
    )
    let harness = IdentificationHarness(currentUser: identity)

    await harness.viewModel.load()

    #expect(harness.completions == 1)
    #expect(await harness.messaging.startCount == 1)
  }

  @Test
  func blankNameStaysOnTheFormWithoutPersistenceOrNetworkWork() async {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = " \n\t "

    harness.viewModel.confirm()

    #expect(harness.viewModel.state == .form)
    #expect(harness.viewModel.validationMessage == "Enter a user name.")
    #expect(harness.users.savedNames.isEmpty)
    #expect(await harness.messaging.startCount == 0)
  }

  @Test
  func validConfirmationSavesBeforeStartingAndPreventsDuplicates() async throws {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = "Alice"

    harness.viewModel.confirm()
    harness.viewModel.confirm()

    #expect(harness.viewModel.state == .loading)
    #expect(harness.users.savedNames == ["Alice"])
    try await eventually { await harness.messaging.startCount == 1 }
  }

  @Test
  func serverAcceptanceCompletesTheActiveAttempt() async throws {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = "Alice"
    harness.viewModel.confirm()
    try await eventually { await harness.messaging.startCount == 1 }

    await harness.messaging.emit(.connected)

    try await eventually { harness.completions == 1 }
  }

  @Test
  func retryableFailureCanRetryWithTheSameSavedIdentity() async throws {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = "Alice"
    harness.viewModel.confirm()
    try await eventually { await harness.messaging.startCount == 1 }

    await harness.messaging.emit(.connectionFailure(.identificationTimedOut))
    try await eventually {
      harness.viewModel.state == .error(
        IdentificationErrorPresentation(
          message: MessagingFailure.identificationTimedOut.localizedDescription,
          allowsRetry: true,
          allowsCancel: true
        )
      )
    }
    let firstID = try #require(harness.users.current?.userId)

    harness.viewModel.retry()

    try await eventually { await harness.messaging.startCount == 2 }
    #expect(harness.users.current?.userId == firstID)
    #expect(harness.users.savedNames == ["Alice", "Alice"])
  }

  @Test
  func cancelReturnsToThePrefilledFormAndIgnoresLaterState() async throws {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = "Alice"
    harness.viewModel.confirm()
    try await eventually { await harness.messaging.startCount == 1 }
    await harness.messaging.emit(.connectionFailure(.network(.transportFailure)))
    try await eventually {
      if case .error = harness.viewModel.state { return true }
      return false
    }

    harness.viewModel.cancel()
    await harness.messaging.emit(.connected)

    #expect(harness.viewModel.state == .form)
    #expect(harness.viewModel.name == "Alice")
    #expect(harness.users.current?.userId == IdentificationFakeUserRepository.fixedID)
    #expect(harness.completions == 0)
  }

  @Test
  func permanentServerFailureDoesNotOfferAnUnchangedRetry() async throws {
    let harness = IdentificationHarness()
    await harness.viewModel.load()
    harness.viewModel.name = "Alice"
    harness.viewModel.confirm()
    try await eventually { await harness.messaging.startCount == 1 }
    let serverError = try ServerError(
      code: "INVALID_EVENT",
      userMessage: "Check your details.",
      isRetryable: false
    )

    await harness.messaging.emit(.connectionFailure(.server(serverError)))

    try await eventually {
      harness.viewModel.state == .error(
        IdentificationErrorPresentation(
          message: "Check your details.",
          allowsRetry: false,
          allowsCancel: true
        )
      )
    }
  }

  @Test
  func initialReadFailureHasRetryButNoUnsafeCancel() async throws {
    let harness = IdentificationHarness()
    harness.users.readError = .readFailed

    await harness.viewModel.load()

    #expect(
      harness.viewModel.state == .error(
        IdentificationErrorPresentation(
          message: PersistenceError.readFailed.localizedDescription,
          allowsRetry: true,
          allowsCancel: false
        )
      )
    )

    harness.users.readError = nil
    harness.viewModel.retry()
    try await eventually { harness.viewModel.state == .form }
  }
}

@MainActor
private final class IdentificationHarness {
  let users: IdentificationFakeUserRepository
  let messaging = IdentificationFakeMessagingService()
  private(set) var completions = 0
  lazy var viewModel = IdentificationViewModel(
    users: users,
    messaging: messaging,
    onRegistrationCompleted: { [weak self] in self?.completions += 1 }
  )

  init(currentUser: LocalUser? = nil) {
    users = IdentificationFakeUserRepository(current: currentUser)
  }
}

@MainActor
private final class IdentificationFakeUserRepository: UserLocalRepository {
  static let fixedID = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!

  var current: LocalUser?
  var readError: PersistenceError?
  var saveError: PersistenceError?
  private(set) var savedNames = [String]()

  init(current: LocalUser?) {
    self.current = current
  }

  func currentUser() throws -> LocalUser? {
    if let readError { throw readError }
    return current
  }

  func user(id: UUID) throws -> LocalUser? {
    current?.userId == id ? current : nil
  }

  func saveIdentity(name: String) throws -> LocalUser {
    if let saveError { throw saveError }
    savedNames.append(name)
    let identity = LocalUser(
      userId: current?.userId ?? Self.fixedID,
      name: name,
      isCurrent: true,
      registrationCompleted: current?.registrationCompleted ?? false
    )
    current = identity
    return identity
  }

  func completeRegistration(acceptedUser: UserDTO) throws {
    current = LocalUser(
      userId: acceptedUser.userId,
      name: acceptedUser.name,
      isCurrent: true,
      registrationCompleted: true
    )
  }

  func upsertKnownUsers(_ users: [UserDTO]) throws { }

  func observeCurrentUser() -> AsyncThrowingStream<LocalUser?, any Error> {
    AsyncThrowingStream { continuation in
      continuation.yield(current)
      continuation.finish()
    }
  }
}

private actor IdentificationFakeMessagingService: MessagingServiceProtocol {
  private var state = MessagingConnectionState.disconnected
  private var observers = [UUID: AsyncStream<MessagingConnectionState>.Continuation]()
  private(set) var startCount = 0
  private(set) var stopCount = 0

  func start() {
    startCount += 1
    setState(.connecting)
  }

  func stop() {
    stopCount += 1
    setState(.disconnected)
  }

  func sendMessage(text: String, receiverId: UUID) throws -> LocalMessage {
    throw PersistenceError.missingIdentity
  }

  func connectionState() -> MessagingConnectionState { state }

  func observeConnectionState() -> AsyncStream<MessagingConnectionState> {
    let id = UUID()
    let (stream, continuation) = AsyncStream<MessagingConnectionState>.makeStream(
      bufferingPolicy: .bufferingNewest(1)
    )
    observers[id] = continuation
    continuation.yield(state)
    return stream
  }

  func observeIssues() -> AsyncStream<MessagingIssue> {
    AsyncStream { $0.finish() }
  }

  func emit(_ state: MessagingConnectionState) {
    setState(state)
  }

  private func setState(_ newState: MessagingConnectionState) {
    state = newState
    for observer in observers.values { observer.yield(newState) }
  }
}
// MARK: - AI Generated - End
