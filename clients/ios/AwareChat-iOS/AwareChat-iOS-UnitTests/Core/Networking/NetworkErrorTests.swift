// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Network Error")
struct NetworkErrorTests {
  @Test
  func serverErrorUsesServerUserMessage() throws {
    let serverError = try ServerError(
      code: "TEMPORARY_UNAVAILABLE",
      userMessage: "Please try again later.",
      isRetryable: true
    )

    #expect(
      NetworkError.server(statusCode: 503, error: serverError).errorDescription
        == "Please try again later."
    )
  }

  @Test
  func localFailureUsesSafeFallback() {
    #expect(
      NetworkError.decoding.errorDescription
        == "Something went wrong. Please try again."
    )
  }

  @Test
  func wrappingPreservesTypedAndTransportErrors() {
    #expect(NetworkError.wrapping(NetworkError.notConnected) == .notConnected)
    #expect(
      NetworkError.wrapping(URLError(.timedOut))
        == .transport(code: .timedOut)
    )
    #expect(NetworkError.wrapping(CancellationError()) == .cancelled)
  }
}
// MARK: - AI Generated - End
