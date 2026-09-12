// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Server Error")
struct ServerErrorTests {
  @Test
  func decodesCompleteErrorAndUsesUserMessageForPresentation() throws {
    let data = Data(
      #"{"code":"TEMPORARY_UNAVAILABLE","userMessage":"Try again later.","developerMessage":"Unavailable for test","isRetryable":true,"requestId":"request-1"}"#.utf8
    )

    let error = try JSONDecoder().decode(ServerError.self, from: data)

    #expect(error.code == "TEMPORARY_UNAVAILABLE")
    #expect(error.errorDescription == "Try again later.")
    #expect(error.developerMessage == "Unavailable for test")
    #expect(error.isRetryable)
    #expect(error.requestId == "request-1")
  }

  @Test(
    "Decode missing and null optional fields",
    arguments: [
      #"{"code":"INVALID_EVENT","userMessage":"Invalid event.","isRetryable":false}"#,
      #"{"code":"INVALID_EVENT","userMessage":"Invalid event.","developerMessage":null,"isRetryable":false,"requestId":null}"#,
    ]
  )
  func decodesMissingAndNullOptionalFields(json: String) throws {
    let error = try JSONDecoder().decode(ServerError.self, from: Data(json.utf8))

    #expect(error.developerMessage == nil)
    #expect(error.requestId == nil)
  }

  @Test
  func acceptsUnknownCodeAndFields() throws {
    let data = Data(
      #"{"code":"FUTURE_CODE","userMessage":"A future error.","isRetryable":true,"futureField":{"value":1}}"#.utf8
    )

    let error = try JSONDecoder().decode(ServerError.self, from: data)

    #expect(error.code == "FUTURE_CODE")
    #expect(error.isRetryable)
  }

  @Test
  func rejectsBlankUserMessage() {
    let data = Data(
      #"{"code":"INVALID_EVENT","userMessage":"   ","isRetryable":false}"#.utf8
    )

    #expect(throws: WireModelError.blankValue) {
      try JSONDecoder().decode(ServerError.self, from: data)
    }
  }
}
// MARK: - AI Generated - End
