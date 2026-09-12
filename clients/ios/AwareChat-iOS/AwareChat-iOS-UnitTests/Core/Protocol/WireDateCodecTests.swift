// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Wire Date Codec")
struct WireDateCodecTests {
  @Test
  func decodesWholeAndFractionalUTCForms() throws {
    let whole = try WireDateCodec.decode("2026-09-10T18:30:00Z")

    #expect(
      abs(
        try WireDateCodec.decode("2026-09-10T18:30:00.1Z")
          .timeIntervalSince(whole) - 0.1
      ) < 0.000_001
    )
    #expect(
      abs(
        try WireDateCodec.decode("2026-09-10T18:30:00.123456+00:00")
          .timeIntervalSince(whole) - 0.123_456
      ) < 0.000_001
    )
  }

  @Test(
    "Reject unsupported timestamp forms",
    arguments: [
      "2026-09-10T19:30:00+01:00",
      "2026-09-10T18:30:00",
      "2026-09-10T18:30:00.1234567Z",
      "2026-09-10 18:30:00Z",
    ]
  )
  func rejectsNonUTCOrUnsupportedTimestampForms(value: String) {
    #expect(throws: WireModelError.invalidDate) {
      try WireDateCodec.decode(value)
    }
  }

  @Test
  func encodeUsesDocumentedUTCForms() throws {
    let fractional = try WireDateCodec.decode("2026-09-10T18:30:00.123Z")
    let whole = try WireDateCodec.decode("2026-09-10T18:30:00Z")

    #expect(WireDateCodec.encode(fractional) == "2026-09-10T18:30:00.123000Z")
    #expect(WireDateCodec.encode(whole) == "2026-09-10T18:30:00Z")
  }
}
// MARK: - AI Generated - End
