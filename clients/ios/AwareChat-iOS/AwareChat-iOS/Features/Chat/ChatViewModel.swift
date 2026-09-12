// MARK: - AI Generated - Start
import Foundation
import Observation

nonisolated enum ChatScreenState: Equatable, Sendable {
  case loading
  case ready([LocalMessage])
  case error(String)
}

nonisolated struct ChatIssuePresentation: Equatable, Sendable {
  let messageId: UUID?
  let message: String
}

@MainActor
struct ChatViewModelFactory {
  let conversations: any ConversationLocalRepository
  let messages: any MessageLocalRepository
  let messaging: any MessagingServiceProtocol

  func make(peerId: UUID) -> ChatViewModel {
    ChatViewModel(
      peerId: peerId,
      conversations: conversations,
      messages: messages,
      messaging: messaging
    )
  }
}

@MainActor
@Observable
final class ChatViewModel {
  private(set) var screenState = ChatScreenState.loading
  private(set) var connectionState = ConnectionStatusState.connecting
  private(set) var peerName = "Unknown user"
  var draft = "" {
    didSet {
      validationMessage = nil
      sendErrorMessage = nil
    }
  }
  private(set) var validationMessage: String?
  private(set) var sendErrorMessage: String?
  private(set) var latestIssue: ChatIssuePresentation?
  private(set) var isSending = false

  var canSend: Bool {
    !isSending
      && draft.contains(where: { !$0.isWhitespace })
      && screenState.isReady
  }

  private let peerId: UUID
  private let conversations: any ConversationLocalRepository
  private let messages: any MessageLocalRepository
  private let messaging: any MessagingServiceProtocol

  private var didLoad = false
  private var conversationId: String?
  private var messageTask: Task<Void, Never>?
  private var connectionTask: Task<Void, Never>?
  private var issueTask: Task<Void, Never>?
  private var connectionRetryTask: Task<Void, Never>?

  init(
    peerId: UUID,
    conversations: any ConversationLocalRepository,
    messages: any MessageLocalRepository,
    messaging: any MessagingServiceProtocol
  ) {
    self.peerId = peerId
    self.conversations = conversations
    self.messages = messages
    self.messaging = messaging
  }

  func load() async {
    guard !didLoad else { return }
    didLoad = true
    screenState = .loading

    do {
      let conversation = try conversations.getOrCreate(with: peerId)
      peerName = conversation.peer.name ?? "Unknown user"
      conversationId = conversation.conversationId
      observeMessages(conversationId: conversation.conversationId)
      observeConnection()
      observeIssues()
    } catch {
      screenState = .error(error.localizedDescription)
    }
  }

  func send() async {
    guard screenState.isReady, !isSending else { return }
    guard draft.contains(where: { !$0.isWhitespace }) else {
      validationMessage = "Enter a message."
      return
    }

    let submittedDraft = draft
    isSending = true
    validationMessage = nil
    sendErrorMessage = nil
    defer { isSending = false }

    do {
      _ = try await messaging.sendMessage(text: submittedDraft, receiverId: peerId)
      if draft == submittedDraft {
        draft = ""
      }
    } catch is CancellationError {
      return
    } catch {
      sendErrorMessage = error.localizedDescription
    }
  }

  func retryLocalLoad() {
    stopObserving()
    screenState = .loading
    Task { await load() }
  }

  func retryConnection() {
    connectionRetryTask?.cancel()
    connectionState = .connecting
    let messaging = messaging
    connectionRetryTask = Task {
      await messaging.stop()
      guard !Task.isCancelled else { return }
      await messaging.start()
    }
  }

  func dismissLatestIssue() {
    latestIssue = nil
  }

  func stopObserving() {
    didLoad = false
    messageTask?.cancel()
    messageTask = nil
    connectionTask?.cancel()
    connectionTask = nil
    issueTask?.cancel()
    issueTask = nil
  }

  private func observeMessages(conversationId: String) {
    messageTask?.cancel()
    let messages = messages
    messageTask = Task { [weak self] in
      do {
        for try await snapshot in messages.observeMessages(conversationId: conversationId) {
          try Task.checkCancellation()
          guard let self, self.conversationId == conversationId else { return }
          clearResolvedIssue(using: snapshot)
          screenState = .ready(snapshot)
        }
      } catch is CancellationError {
        return
      } catch {
        guard let self, self.conversationId == conversationId else { return }
        screenState = .error(error.localizedDescription)
      }
    }
  }

  private func observeConnection() {
    connectionTask?.cancel()
    let messaging = messaging
    connectionTask = Task { [weak self] in
      let updates = await messaging.observeConnectionState()
      for await update in updates {
        guard !Task.isCancelled, let self else { return }
        connectionState = Self.presentationState(update)
      }
    }
  }

  private func observeIssues() {
    issueTask?.cancel()
    let messaging = messaging
    issueTask = Task { [weak self] in
      let updates = await messaging.observeIssues()
      for await issue in updates {
        guard !Task.isCancelled, let self else { return }
        latestIssue = ChatIssuePresentation(
          messageId: issue.messageId,
          message: issue.error.userMessage
        )
      }
    }
  }

  private func clearResolvedIssue(using messages: [LocalMessage]) {
    guard let issueId = latestIssue?.messageId,
          messages.contains(where: { $0.id == issueId && $0.state == .sent }) else {
      return
    }
    latestIssue = nil
  }

  private static func presentationState(
    _ state: MessagingConnectionState
  ) -> ConnectionStatusState {
    switch state {
    case .connected:
      .connected
    case .connecting:
      .connecting
    case .disconnected:
      .offline("You are offline. Messages will be sent when you reconnect.")
    case .connectionFailure(let failure):
      .offline(failure.localizedDescription)
    }
  }
}

private extension ChatScreenState {
  var isReady: Bool {
    if case .ready = self { return true }
    return false
  }
}
// MARK: - AI Generated - End
