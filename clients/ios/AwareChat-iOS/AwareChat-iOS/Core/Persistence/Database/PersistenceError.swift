import Foundation

nonisolated enum PersistenceError: Error, Equatable, Sendable, LocalizedError {
  case readFailed
  case writeFailed
  case invalidData
  case missingIdentity
  case identityConflict
  case messageConflict
  case messageNotFound
  case sequenceExhausted

  var errorDescription: String? {
    switch self {
    case .readFailed: "Your saved data could not be loaded. Please try again."
    case .writeFailed: "Your data could not be saved. Please try again."
    case .missingIdentity: "Save your name before sending messages."
    default: "The local data could not be updated safely."
    }
  }
}
