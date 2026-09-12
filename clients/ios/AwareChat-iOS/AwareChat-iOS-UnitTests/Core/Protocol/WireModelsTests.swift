// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@Suite("Wire Models")
struct WireModelsTests {
  private let alice = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!
  private let bob = UUID(uuidString: "22222222-2222-4222-8222-222222222222")!

  @Test
  func userRejectsBlankNameAndEncodesNormalizedID() throws {
    #expect(throws: WireModelError.blankValue) {
      try UserDTO(userId: alice, name: " \n ")
    }

    let user = try UserDTO(userId: alice, name: "Alice")
    let object = try #require(
      JSONSerialization.jsonObject(with: JSONEncoder().encode(user)) as? [String: Any]
    )

    #expect(object["userId"] as? String == alice.uuidString.lowercased())
    #expect(object["name"] as? String == "Alice")
  }

  @Test
  func messageBuildsCanonicalConversationAndEncodesNullServerTimestamp() throws {
    let message = try MessageDTO(
      messageId: UUID(uuidString: "85d983ab-9592-444c-9046-25046ca9b770")!,
      text: "Hi Bob",
      senderId: bob,
      receiverId: alice,
      clientCreatedAt: Date(timeIntervalSince1970: 0),
      clientSequence: 4
    )
    let object = try #require(
      JSONSerialization.jsonObject(with: JSONEncoder().encode(message)) as? [String: Any]
    )

    #expect(
      message.conversationId
        == "11111111-1111-4111-8111-111111111111:22222222-2222-4222-8222-222222222222"
    )
    #expect(object["serverReceivedAt"] is NSNull)
  }

  @Test
  func messageRejectsInvalidSequenceAndSelfSend() {
    #expect(throws: WireModelError.invalidSequence) {
      try MessageDTO(
        messageId: UUID(),
        text: "Hello",
        senderId: alice,
        receiverId: bob,
        clientCreatedAt: Date(),
        clientSequence: 0
      )
    }
    #expect(throws: WireModelError.invalidConversation) {
      try MessageDTO(
        messageId: UUID(),
        text: "Hello",
        senderId: alice,
        receiverId: alice,
        clientCreatedAt: Date(),
        clientSequence: 1
      )
    }
  }

  @Test
  func decodeRejectsIncorrectConversationID() {
    let data = Data(
      #"{"messageId":"85d983ab-9592-444c-9046-25046ca9b770","conversationId":"wrong","text":"Hi","senderId":"11111111-1111-4111-8111-111111111111","receiverId":"22222222-2222-4222-8222-222222222222","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":null}"#.utf8
    )

    #expect(throws: WireModelError.invalidConversation) {
      try JSONDecoder().decode(MessageDTO.self, from: data)
    }
  }

  @Test
  func healthRequiresExpectedStatusAndProtocolVersion() throws {
    let valid = try JSONDecoder().decode(
      HealthResponse.self,
      from: Data(#"{"status":"ok","protocolVersion":1}"#.utf8)
    )
    #expect(valid.status == "ok")

    #expect(throws: WireModelError.invalidHealthResponse) {
      try JSONDecoder().decode(
        HealthResponse.self,
        from: Data(#"{"status":"ok","protocolVersion":2}"#.utf8)
      )
    }
  }
}
// MARK: - AI Generated - End
