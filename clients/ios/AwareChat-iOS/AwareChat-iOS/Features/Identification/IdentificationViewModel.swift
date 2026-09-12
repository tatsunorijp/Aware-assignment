// MARK: - AI Generated - Start
import Foundation
import Observation

nonisolated struct IdentificationErrorPresentation: Equatable, Sendable {
  let message: String
  let allowsRetry: Bool
  let allowsCancel: Bool
}

nonisolated enum IdentificationState: Equatable, Sendable {
  case loading
  case form
  case error(IdentificationErrorPresentation)
}

@MainActor
@Observable
final class IdentificationViewModel {
  private(set) var state = IdentificationState.loading
  var name = "" {
    didSet { validationMessage = nil }
  }
  private(set) var validationMessage: String?

  private let users: any UserLocalRepository
  private let messaging: any MessagingServiceProtocol
  private let onRegistrationCompleted: () -> Void
  private var didLoad = false
  private var failedOperation: FailedOperation?
  private var activeAttempt: UUID?
  private var registrationTask: Task<Void, Never>?

  init(
    users: any UserLocalRepository,
    messaging: any MessagingServiceProtocol,
    onRegistrationCompleted: @escaping () -> Void
  ) {
    self.users = users
    self.messaging = messaging
    self.onRegistrationCompleted = onRegistrationCompleted
  }

  func load() async {
    guard !didLoad else { return }
    didLoad = true
    state = .loading

    do {
      let currentUser = try users.currentUser()
      name = currentUser?.name ?? ""
      if currentUser?.registrationCompleted == true {
        await messaging.start()
        onRegistrationCompleted()
      } else {
        state = .form
      }
    } catch {
      present(error, operation: .loadIdentity)
    }
  }

  func confirm() {
    guard state == .form else { return }
    guard name.contains(where: { !$0.isWhitespace }) else {
      validationMessage = "Enter a user name."
      return
    }

    beginRegistration()
  }

  func retry() {
    guard case .error(let presentation) = state, presentation.allowsRetry,
          let failedOperation else { return }

    switch failedOperation {
    case .loadIdentity:
      didLoad = false
      Task { await load() }
    case .registration:
      beginRegistration()
    }
  }

  func cancel() {
    guard case .error(let presentation) = state, presentation.allowsCancel else { return }
    invalidateAttempt()
    failedOperation = nil
    state = .form
    let messaging = messaging
    Task { await messaging.stop() }
  }

  private func beginRegistration() {
    state = .loading
    validationMessage = nil

    do {
      _ = try users.saveIdentity(name: name)
    } catch {
      present(error, operation: .registration)
      return
    }

    invalidateAttempt()
    let attempt = UUID()
    activeAttempt = attempt
    registrationTask = Task { [weak self] in
      await self?.register(attempt: attempt)
    }
  }

  private func register(attempt: UUID) async {
    let connectionStates = await messaging.observeConnectionState()
    guard isActive(attempt) else { return }
    await messaging.start()

    for await connectionState in connectionStates {
      guard isActive(attempt) else { return }

      switch connectionState {
      case .connected:
        finish(attempt)
        onRegistrationCompleted()
        return
      case .connectionFailure(let failure):
        await messaging.stop()
        guard isActive(attempt) else { return }
        finish(attempt)
        present(failure, operation: .registration)
        return
      case .disconnected, .connecting:
        continue
      }
    }
  }

  private func present(_ error: any Error, operation: FailedOperation) {
    failedOperation = operation
    state = .error(
      IdentificationErrorPresentation(
        message: error.localizedDescription,
        allowsRetry: allowsRetry(error),
        allowsCancel: operation == .registration
      )
    )
  }

  private func allowsRetry(_ error: any Error) -> Bool {
    if let failure = error as? MessagingFailure {
      switch failure {
      case .server(let serverError):
        return serverError.isRetryable
      case .storage(let persistenceError):
        return allowsRetry(persistenceError)
      case .unexpectedIdentity:
        return false
      case .network, .identificationTimedOut, .acceptanceTimedOut, .sessionReplaced:
        return true
      }
    }

    if let persistenceError = error as? PersistenceError {
      return allowsRetry(persistenceError)
    }

    return true
  }

  private func allowsRetry(_ error: PersistenceError) -> Bool {
    switch error {
    case .readFailed, .writeFailed:
      return true
    case .invalidData, .missingIdentity, .identityConflict, .messageConflict,
         .messageNotFound, .sequenceExhausted:
      return false
    }
  }

  private func isActive(_ attempt: UUID) -> Bool {
    activeAttempt == attempt && !Task.isCancelled
  }

  private func finish(_ attempt: UUID) {
    guard activeAttempt == attempt else { return }
    activeAttempt = nil
    registrationTask = nil
  }

  private func invalidateAttempt() {
    activeAttempt = nil
    registrationTask?.cancel()
    registrationTask = nil
  }

  private enum FailedOperation: Equatable {
    case loadIdentity
    case registration
  }
}
// MARK: - AI Generated - End
