// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("Offline messaging service")
struct MessagingServiceTests {
  @Test func offlineQueueFlushesFIFOOnlyAfterPersistedIdentificationWithoutSyncGate() async throws {
    let h = try MessagingTestHarness()
    let peer = UUID()
    let first = try await h.service.sendMessage(text: "First", receiverId: peer)
    let second = try await h.service.sendMessage(text: "Second", receiverId: peer)
    #expect(await h.socket.sent.isEmpty)
    #expect(first.state == .pendingToSend)
    await h.service.start()
    await h.service.start()
    try await eventually { await h.socket.sent.count == 1 }
    #expect(try h.store.users.currentUser()?.registrationCompleted == false)
    #expect(await h.socket.sentMessages().isEmpty)
    await h.socket.emit(.syncCompleted(pendingCount: 0))
    await h.socket.emit(.identityAccepted(user: h.user))
    try await eventually { await h.socket.sentMessages().count == 1 }
    #expect(try h.store.users.currentUser()?.registrationCompleted == true)
    #expect(await h.socket.sentMessages().map(\.messageId) == [first.id])
    await h.socket.emit(.messageAccepted(messageId: first.id, serverReceivedAt: .now))
    try await eventually { await h.socket.sentMessages().count == 2 }
    #expect(await h.socket.sentMessages().map(\.messageId) == [first.id, second.id])
    #expect(try h.store.messages.message(id: first.id)?.state == .sent)
    #expect(await h.socket.connections == 1)
    await h.service.stop()
    #expect(try h.store.messages.message(id: second.id)?.state == .pendingToSend)
  }

  @Test func lostACKReconnectsAndRetriesImmutablePayloadWithProgressiveDelay() async throws {
    let h = try MessagingTestHarness()
    let local = try await h.service.sendMessage(text: "Retry me", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    try await h.advance(.seconds(10))
    try await eventually { try h.store.messages.message(id: local.id)?.state == .pendingToSend }
    try await h.advance(.seconds(1))
    try await eventually { await h.socket.connections == 2 }
    await h.socket.emit(.identityAccepted(user: h.user))
    try await eventually { await h.socket.sentMessages().count == 2 }
    #expect(await h.socket.sentMessages() == [local.message, local.message])
    try await h.advance(.seconds(10))
    try await eventually { await h.clock.hasWaiter(for: .seconds(2)) }
    #expect(try h.store.messages.message(id: local.id)?.state == .pendingToSend)
    await h.service.stop()
  }

  @Test func duplicateIncomingIsAcknowledgedAgainAndNeverInsertedTwice() async throws {
    let h = try MessagingTestHarness()
    try await h.connect()
    let incoming = try h.store.incoming()
    await h.socket.emit(.incomingMessage(message: incoming))
    try await eventually { await h.socket.acknowledgementCount(id: incoming.messageId) == 1 }
    let first = try h.store.messages.message(id: incoming.messageId)
    #expect(first?.receivedAt == h.clock.now())
    await h.socket.emit(.incomingMessage(message: incoming))
    try await eventually { await h.socket.acknowledgementCount(id: incoming.messageId) == 2 }
    #expect(try h.store.messages.messages(conversationId: incoming.conversationId) == [first!])
    await h.service.stop()
  }

  @Test func incomingSaveFailureSendsNoACKAndExplicitRestartAllowsReplay() async throws {
    let h = try MessagingTestHarness()
    try await h.connect()
    let incoming = try h.store.incoming()
    h.store.writes.failNext = true
    await h.socket.emit(.incomingMessage(message: incoming))
    try await eventually { await h.service.connectionState() == .connectionFailure(.storage(.writeFailed)) }
    #expect(await h.socket.acknowledgementCount(id: incoming.messageId) == 0)
    #expect(try h.store.messages.message(id: incoming.messageId) == nil)
    await h.service.stop()
    try await h.connect()
    await h.socket.emit(.incomingMessage(message: incoming))
    try await eventually { await h.socket.acknowledgementCount(id: incoming.messageId) == 1 }
    await h.service.stop()
  }

  @Test func completionSaveFailureDoesNotPublishReadyOrSendOutbox() async throws {
    let h = try MessagingTestHarness()
    _ = try await h.service.sendMessage(text: "Pending", receiverId: UUID())
    await h.service.start()
    try await eventually { await h.socket.sent.count == 1 }
    h.store.writes.failNext = true
    await h.socket.emit(.identityAccepted(user: h.user))
    try await eventually { await h.service.connectionState() == .connectionFailure(.storage(.writeFailed)) }
    #expect(try h.store.users.currentUser()?.registrationCompleted == false)
    #expect(await h.socket.sentMessages().isEmpty)
    await h.service.stop()
  }

  @Test func temporaryAndPermanentErrorsUseCorrelationWithoutDowngradingSent() async throws {
    let h = try MessagingTestHarness()
    let first = try await h.service.sendMessage(text: "First", receiverId: UUID())
    let second = try await h.service.sendMessage(text: "Second", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    let temporary = try ServerError(code: "FUTURE_TEMPORARY_CODE", userMessage: "Try later", isRetryable: true)
    let permanent = try ServerError(code: "FUTURE_PERMANENT_CODE", userMessage: "Cannot send", isRetryable: false)
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: first.id.uuidString, error: temporary)))
    try await eventually { try h.store.messages.message(id: first.id)?.state == .pendingToSend }
    #expect(await h.socket.sentMessages().count == 1)
    try await h.advance(.seconds(1))
    try await eventually { await h.socket.sentMessages().count == 2 }
    await h.socket.emit(.messageAccepted(messageId: first.id, serverReceivedAt: .now))
    try await eventually { await h.socket.sentMessages().count == 3 }
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: first.id.uuidString, error: permanent)))
    let recipientError = try ServerError(code: "NOT_RECEIVER", userMessage: "Invalid receipt", isRetryable: false)
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: second.id.uuidString, error: recipientError)))
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: second.id.uuidString, error: permanent)))
    try await eventually { try h.store.messages.message(id: second.id)?.state == .failed }
    #expect(try h.store.messages.message(id: first.id)?.state == .sent)
    #expect(try h.store.messages.pendingMessages().isEmpty)
    await h.service.stop()
  }

  @Test func sessionReplacementStopsAutomaticReconnectAndPreservesOutbox() async throws {
    let h = try MessagingTestHarness()
    let message = try await h.service.sendMessage(text: "Keep me", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    await h.socket.loseConnection(.webSocketClosed(code: 4001, reason: "Session replaced"))
    try await eventually { await h.service.connectionState() == .connectionFailure(.sessionReplaced) }
    try await eventually { try h.store.messages.message(id: message.id)?.state == .pendingToSend }
    #expect(await h.socket.connections == 1)
    #expect(await h.clock.hasWaiter(for: .seconds(1)) == false)
    await h.service.stop()
  }

  @Test func failedTransportWriteReturnsSendingToPendingAndCanBeStoppedDuringBackoff() async throws {
    let h = try MessagingTestHarness()
    try await h.connect()
    await h.socket.rejectNextWrite()
    let message = try await h.service.sendMessage(text: "Saved before network", receiverId: UUID())
    try await eventually {
      await h.service.connectionState() == .connectionFailure(.network(.transportFailure))
    }
    #expect(try h.store.messages.message(id: message.id)?.state == .pendingToSend)
    await h.service.stop()
    await h.clock.advance(.seconds(1))
    #expect(await h.service.connectionState() == .disconnected)
    #expect(await h.socket.connections == 1)
  }

  @Test func identificationTimeoutIsDistinctAndDoesNotCompleteRegistration() async throws {
    let h = try MessagingTestHarness()
    await h.service.start()
    try await eventually { await h.socket.sent.count == 1 }
    try await h.advance(.seconds(10))
    try await eventually { await h.service.connectionState() == .connectionFailure(.identificationTimedOut) }
    #expect(try h.store.users.currentUser()?.registrationCompleted == false)
    try await h.advance(.seconds(1))
    try await eventually { await h.socket.connections == 2 }
    #expect(await h.socket.sent.allSatisfy { $0 == .identify(user: h.user) })
    await h.service.stop()
  }

  @Test func uncorrelatedPermanentErrorPreservesMessagesAndPublishesSafeTypedIssue() async throws {
    let h = try MessagingTestHarness()
    let message = try await h.service.sendMessage(text: "Keep pending", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    var issues = await h.service.observeIssues().makeAsyncIterator()
    let error = try ServerError(code: "INVALID_EVENT", userMessage: "Please reconnect",
                                developerMessage: "Private diagnostic", isRetryable: false)
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: nil, error: error)))
    #expect(await issues.next() == MessagingIssue(messageId: nil, error: error))
    try await eventually { await h.service.connectionState() == .connectionFailure(.server(error)) }
    try await eventually { try h.store.messages.message(id: message.id)?.state == .pendingToSend }
    #expect(MessagingFailure.server(error).localizedDescription == "Please reconnect")
    await h.service.stop()
  }

  @Test func acceptanceSaveFailurePreservesOriginalPayloadForNextLaunch() async throws {
    let h = try MessagingTestHarness()
    let message = try await h.service.sendMessage(text: "Save the ACK", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    h.store.writes.failNext = true
    await h.socket.emit(.messageAccepted(messageId: message.id, serverReceivedAt: .now))
    try await eventually { await h.service.connectionState() == .connectionFailure(.storage(.writeFailed)) }
    try await eventually { try h.store.messages.message(id: message.id)?.state == .pendingToSend }
    #expect(try h.store.messages.message(id: message.id)?.message == message.message)
    await h.service.stop()
  }

  @Test func receivedACKErrorDoesNotFailOutgoingAndAcceptedHistoryIsNotReplayed() async throws {
    let h = try MessagingTestHarness()
    let message = try await h.service.sendMessage(text: "Only once", receiverId: UUID())
    try await h.connect()
    try await eventually { await h.socket.sentMessages().count == 1 }
    var issues = await h.service.observeIssues().makeAsyncIterator()
    let error = try ServerError(code: "UNKNOWN_MESSAGE", userMessage: "Receipt not found", isRetryable: false)
    await h.socket.emit(.protocolError(ProtocolErrorEvent(messageId: message.id.uuidString, error: error)))
    _ = await issues.next()
    #expect(try h.store.messages.message(id: message.id)?.state == .sending)
    await h.socket.emit(.messageAccepted(messageId: message.id, serverReceivedAt: .now))
    try await eventually { try h.store.messages.message(id: message.id)?.state == .sent }
    await h.service.stop()
    try await h.connect()
    #expect(await h.socket.sentMessages().count == 1)
    #expect(try h.store.users.currentUser()?.registrationCompleted == true)
    await h.service.stop()
  }
}
// MARK: - AI Generated - End
