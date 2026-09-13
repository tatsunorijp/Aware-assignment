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
@Observable
final class ChatViewModel {
  private(set) var screenState = ChatScreenState.loading
  private(set) var peerName = "Unknown user"
  private(set) var connectionState = ConnectionStatusState.connecting
  private(set) var validationMessage: String?
  private(set) var sendErrorMessage: String?
  private(set) var latestIssue: ChatIssuePresentation?
  private(set) var isSending = false

  var draft = "" {
    didSet {
      validationMessage = nil
      sendErrorMessage = nil
    }
  }

  var canSend: Bool {
    guard case .ready = screenState else { return false }
    return !isSending && draft.contains { !$0.isWhitespace }
  }

  private let peerId: UUID
  private let conversations: any ConversationLocalRepository
  private let messages: any MessageLocalRepository
  private let messaging: any MessagingServiceProtocol

  private var didLoad = false
  private var historyTask: Task<Void, Never>?
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
    observeConnection()
    observeIssues()
    observeHistory()
  }

  func retryLocalLoad() {
    screenState = .loading
    observeHistory()
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

  func send() async {
    guard !isSending else { return }
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

  func dismissLatestIssue() {
    latestIssue = nil
  }

  func stopObserving() {
    didLoad = false
    historyTask?.cancel()
    historyTask = nil
    connectionTask?.cancel()
    connectionTask = nil
    issueTask?.cancel()
    issueTask = nil
  }

  private func observeHistory() {
    historyTask?.cancel()

    do {
      let conversation = try conversations.getOrCreate(with: peerId)
      peerName = conversation.peer.name ?? "Unknown user"
      let updates = messages.observeMessages(conversationId: conversation.conversationId)
      historyTask = Task { [weak self] in
        do {
          for try await snapshot in updates {
            try Task.checkCancellation()
            guard let self else { return }
            screenState = .ready(snapshot)
            clearResolvedIssue(using: snapshot)
          }
        } catch is CancellationError {
          return
        } catch {
          guard let self else { return }
          screenState = .error(error.localizedDescription)
        }
      }
    } catch {
      screenState = .error(error.localizedDescription)
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
        if let messageId = issue.messageId,
           case .ready(let messages) = screenState,
           messages.contains(where: { $0.id == messageId && $0.state == .sent }) {
          continue
        }
        latestIssue = ChatIssuePresentation(
          messageId: issue.messageId,
          message: issue.error.userMessage
        )
      }
    }
  }

  private func clearResolvedIssue(using messages: [LocalMessage]) {
    guard let messageId = latestIssue?.messageId else { return }
    if messages.contains(where: { $0.id == messageId && $0.state == .sent }) {
      latestIssue = nil
    }
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
      .offline("You are offline.")
    case .connectionFailure(let failure):
      .offline(failure.localizedDescription)
    }
  }
}

@MainActor
struct ChatViewModelFactory {
  private let conversations: any ConversationLocalRepository
  private let messages: any MessageLocalRepository
  private let messaging: any MessagingServiceProtocol

  init(
    conversations: any ConversationLocalRepository,
    messages: any MessageLocalRepository,
    messaging: any MessagingServiceProtocol
  ) {
    self.conversations = conversations
    self.messages = messages
    self.messaging = messaging
  }

  func make(peerId: UUID) -> ChatViewModel {
    ChatViewModel(
      peerId: peerId,
      conversations: conversations,
      messages: messages,
      messaging: messaging
    )
  }
}
// MARK: - AI Generated - End
