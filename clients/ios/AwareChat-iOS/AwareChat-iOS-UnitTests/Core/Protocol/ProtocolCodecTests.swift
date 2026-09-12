// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Protocol Codec")
struct ProtocolCodecTests {
  private let codec = ProtocolCodec()
  private let alice = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!
  private let bob = UUID(uuidString: "22222222-2222-4222-8222-222222222222")!

  @Test
  func encodesEveryClientEvent() throws {
    let user = try UserDTO(userId: alice, name: "Alice")
    let messageId = UUID(uuidString: "85d983ab-9592-444c-9046-25046ca9b770")!
    let message = try MessageDTO(
      messageId: messageId,
      text: "Hi Bob",
      senderId: alice,
      receiverId: bob,
      clientCreatedAt: try WireDateCodec.decode("2026-09-10T18:30:00Z"),
      clientSequence: 4
    )

    let events: [(ClientEvent, String)] = [
      (.identify(user: user), "identify"),
      (.sendMessage(message: message), "send_message"),
      (.messagePersisted(messageId: messageId), "message_persisted"),
    ]

    for (event, expectedType) in events {
      let object = try #require(
        JSONSerialization.jsonObject(with: codec.encode(event)) as? [String: Any]
      )
      #expect(object["type"] as? String == expectedType)
      #expect(object["protocolVersion"] as? Int == 1)
    }
  }

  @Test
  func decodesSuccessfulServerEvents() throws {
    let identity = try codec.decodeServerEvent(
      from: Data(
        #"{"type":"identity_accepted","protocolVersion":1,"user":{"userId":"11111111-1111-4111-8111-111111111111","name":"Alice"}}"#.utf8
      )
    )
    let sync = try codec.decodeServerEvent(
      from: Data(#"{"type":"sync_completed","protocolVersion":1,"pendingCount":2}"#.utf8)
    )
    let accepted = try codec.decodeServerEvent(
      from: Data(
        #"{"type":"message_accepted","protocolVersion":1,"messageId":"85d983ab-9592-444c-9046-25046ca9b770","serverReceivedAt":"2026-09-10T18:30:00.123456Z"}"#.utf8
      )
    )
    let incoming = try codec.decodeServerEvent(from: incomingMessageData())

    #expect(identity == .identityAccepted(user: try UserDTO(userId: alice, name: "Alice")))
    #expect(sync == .syncCompleted(pendingCount: 2))
    guard case .messageAccepted(let messageId, _) = accepted else {
      Issue.record("Expected message_accepted")
      return
    }
    #expect(messageId == UUID(uuidString: "85d983ab-9592-444c-9046-25046ca9b770"))
    guard case .incomingMessage(let message) = incoming else {
      Issue.record("Expected incoming_message")
      return
    }
    #expect(message.serverReceivedAt != nil)
  }

  @Test
  func decodesMaintainedProtocolErrorFixtures() throws {
    let fixtures: [(name: String, messageId: String?, code: String, retryable: Bool)] = [
      (
        "protocol_error_complete.json",
        "85D983AB-9592-444C-9046-25046CA9B770",
        "INVALID_MESSAGE",
        false
      ),
      ("protocol_error_minimal.json", nil, "INVALID_EVENT", false),
      ("protocol_error_null_optionals.json", nil, "IDENTIFICATION_REQUIRED", true),
      (
        "protocol_error_unknown_code.json",
        "85D983AB-9592-444C-9046-25046CA9B770",
        "FUTURE_SERVER_CONDITION",
        true
      ),
    ]

    for fixture in fixtures {
      let event = try codec.decodeServerEvent(from: fixtureData(named: fixture.name))
      guard case .protocolError(let protocolError) = event else {
        Issue.record("Expected protocol_error for \(fixture.name)")
        return
      }
      #expect(protocolError.messageId == fixture.messageId)
      #expect(protocolError.error.code == fixture.code)
      #expect(protocolError.error.isRetryable == fixture.retryable)
      #expect(!protocolError.error.userMessage.isEmpty)
    }
  }

  @Test(
    "Reject unsupported or invalid server events",
    arguments: [
      #"{"type":"sync_completed","protocolVersion":2,"pendingCount":0}"#,
      #"{"type":"future_event","protocolVersion":1}"#,
      #"{"type":"sync_completed","protocolVersion":1,"pendingCount":-1}"#,
      #"{"type":"incoming_message","protocolVersion":1,"message":{"messageId":"85d983ab-9592-444c-9046-25046ca9b770","conversationId":"11111111-1111-4111-8111-111111111111:22222222-2222-4222-8222-222222222222","text":"Hi","senderId":"11111111-1111-4111-8111-111111111111","receiverId":"22222222-2222-4222-8222-222222222222","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":null}}"#,
    ]
  )
  func rejectsUnsupportedVersionUnknownTypeAndInvalidPayload(event: String) {
    do {
      _ = try codec.decodeServerEvent(from: Data(event.utf8))
      Issue.record("Expected decoding to fail")
    } catch {
      // Any decoding or validated wire-model error is the expected outcome.
    }
  }

  private func incomingMessageData() -> Data {
    Data(
      #"{"type":"incoming_message","protocolVersion":1,"message":{"messageId":"85d983ab-9592-444c-9046-25046ca9b770","conversationId":"11111111-1111-4111-8111-111111111111:22222222-2222-4222-8222-222222222222","text":"Hi","senderId":"11111111-1111-4111-8111-111111111111","receiverId":"22222222-2222-4222-8222-222222222222","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":"2026-09-10T18:30:00.123456Z"}}"#.utf8
    )
  }

  private func fixtureData(named name: String) throws -> Data {
    var url = URL(fileURLWithPath: #filePath)
    while url.lastPathComponent != "Aware-assignment", url.path != "/" {
      url.deleteLastPathComponent()
    }
    return try Data(contentsOf: url.appendingPathComponent("fixtures/protocol/\(name)"))
  }
}
// MARK: - AI Generated - End
