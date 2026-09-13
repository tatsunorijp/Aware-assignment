// MARK: - AI Generated - Start
import SwiftUI

struct ChatView: View {
  private enum Constants {
    static let maximumContentWidth: CGFloat = 700
    static let messageOppositeInset: CGFloat = 48
    static let composerCornerRadius: CGFloat = 24
    static let sendButtonSize: CGFloat = 48
    static let overlaidBannerSpacing: CGFloat = 96
  }

  @State private var viewModel: ChatViewModel
  @State private var didPositionInitialHistory = false
  @FocusState private var isComposerFocused: Bool
  let onCancel: () -> Void

  init(
    viewModel: ChatViewModel,
    onCancel: @escaping () -> Void
  ) {
    _viewModel = State(initialValue: viewModel)
    self.onCancel = onCancel
  }

  var body: some View {
    Group {
      switch viewModel.screenState {
      case .loading:
        LoadingScreen()
      case .error(let message):
        ErrorScreen(
          message: message,
          retryAction: { viewModel.retryLocalLoad() },
          cancelAction: onCancel
        )
      case .ready(let messages):
        content(messages: messages)
      }
    }
    .navigationBarBackButtonHidden(true)
    .navigationTitle(viewModel.peerName)
    .navigationBarTitleDisplayMode(.inline)
    .toolbar {
      ToolbarItem(placement: .topBarLeading) {
        Button(action: onCancel) {
          Image(systemName: "chevron.left")
        }
        .accessibilityLabel("Back")
      }
    }
    .task { await viewModel.load() }
    .onDisappear { viewModel.stopObserving() }
  }

  private func content(messages: [LocalMessage]) -> some View {
    ScrollViewReader { proxy in
      ScrollView {
        LazyVStack(spacing: Tokens.Spacing.medium.value) {
          ForEach(messages) { message in
            messageRow(message)
              .id(message.id)
          }
        }
        .padding(.horizontal, Tokens.Spacing.medium.value)
        .padding(.top, historyTopPadding)
        .padding(.bottom, Tokens.Spacing.medium.value)
        .frame(maxWidth: Constants.maximumContentWidth)
        .frame(maxWidth: .infinity)
      }
      .scrollDismissesKeyboard(.interactively)
      .background(Tokens.Colors.background.ignoresSafeArea())
      .overlay(alignment: .top) {
        statusOverlay
          .zIndex(1)
      }
      .safeAreaInset(edge: .bottom, spacing: 0) {
        composer
      }
      .onAppear {
        positionInitialHistory(messages, with: proxy)
      }
      .onChange(of: messages) { _, updatedMessages in
        positionInitialHistory(updatedMessages, with: proxy)
      }
      .onChange(of: isComposerFocused) { _, isFocused in
        guard isFocused, let newest = messages.last else { return }
        withAnimation { proxy.scrollTo(newest.id, anchor: .bottom) }
      }
    }
  }

  private func messageRow(_ message: LocalMessage) -> some View {
    VStack(alignment: message.direction == .outgoing ? .trailing : .leading) {
      MessageContainer(
        origin: message.direction == .outgoing ? .sended : .received,
        text: message.message.text,
        date: message.displayTimestamp,
        ackState: ackState(for: message)
      )

      if viewModel.latestIssue?.messageId == message.id,
         let issue = viewModel.latestIssue {
        issueView(issue)
      }
    }
    .padding(
      message.direction == .outgoing ? .leading : .trailing,
      Constants.messageOppositeInset
    )
  }

  private var statusOverlay: some View {
    VStack(spacing: Tokens.Spacing.small.value) {
      ConnectionStatusView(
        state: viewModel.connectionState,
        retryAction: { viewModel.retryConnection() }
      )

      if let issue = viewModel.latestIssue, issue.messageId == nil {
        issueView(issue)
      }
    }
    .padding(.horizontal, Tokens.Spacing.medium.value)
    .padding(.top, Tokens.Spacing.small.value)
    .frame(maxWidth: Constants.maximumContentWidth)
  }

  private func issueView(_ issue: ChatIssuePresentation) -> some View {
    HStack(alignment: .firstTextBaseline, spacing: Tokens.Spacing.small.value) {
      BodyText(issue.message)
        .foregroundStyle(Tokens.Colors.textPrimary)
        .frame(maxWidth: .infinity, alignment: .leading)

      Button("Dismiss") {
        viewModel.dismissLatestIssue()
      }
      .font(.body.weight(.semibold))
      .foregroundStyle(Tokens.Colors.primary)
    }
    .padding(Tokens.Spacing.small.value)
    .background(Tokens.Colors.secondary)
    .clipShape(RoundedRectangle(cornerRadius: Tokens.CornerRadius.medium))
  }

  private var composer: some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
      if let validationMessage = viewModel.validationMessage {
        composerError(validationMessage)
      } else if let sendErrorMessage = viewModel.sendErrorMessage {
        composerError(sendErrorMessage)
      }

      HStack(alignment: .bottom, spacing: Tokens.Spacing.small.value) {
        TextField(
          "Type a message...",
          text: $viewModel.draft,
          prompt: Text("Type a message...").foregroundColor(Tokens.Colors.textSecondary),
          axis: .vertical
        )
        .font(.body)
        .foregroundStyle(Tokens.Colors.textPrimary)
        .lineLimit(1...5)
        .focused($isComposerFocused)
        .submitLabel(.send)
        .onSubmit(send)
        .padding(.horizontal, Tokens.Spacing.medium.value)
        .padding(.vertical, Tokens.Spacing.small.value)
        .background(Tokens.Colors.secondary)
        .clipShape(RoundedRectangle(cornerRadius: Constants.composerCornerRadius))
        .accessibilityLabel("Message")

        Button(action: send) {
          Image(systemName: "paperplane.fill")
            .font(.headline)
            .foregroundStyle(Tokens.Colors.background)
            .frame(width: Constants.sendButtonSize, height: Constants.sendButtonSize)
            .background(Tokens.Colors.primary)
            .clipShape(Circle())
        }
        .disabled(!viewModel.canSend)
        .opacity(viewModel.canSend ? 1 : 0.35)
        .accessibilityLabel("Send message")
      }
    }
    .padding(.horizontal, Tokens.Spacing.medium.value)
    .padding(.vertical, Tokens.Spacing.small.value)
    .background(Tokens.Colors.background)
    .overlay(alignment: .top) {
      Divider().overlay(Tokens.Colors.divider)
    }
  }

  private func composerError(_ message: String) -> some View {
    BodyText(message)
      .foregroundStyle(Tokens.Colors.textPrimary)
      .accessibilityLabel("Error: \(message)")
  }

  private var historyTopPadding: CGFloat {
    switch viewModel.connectionState {
    case .connected:
      viewModel.latestIssue?.messageId == nil && viewModel.latestIssue != nil
        ? Constants.overlaidBannerSpacing : Tokens.Spacing.medium.value
    case .connecting, .offline:
      Constants.overlaidBannerSpacing
    }
  }

  private func ackState(for message: LocalMessage) -> ACKMessageState {
    switch message.state {
    case .sent:
      .sent
    case .failed:
      .failed
    case .pendingToSend, .sending, .none:
      .sending
    }
  }

  private func send() {
    Task { await viewModel.send() }
  }

  private func positionInitialHistory(
    _ messages: [LocalMessage],
    with proxy: ScrollViewProxy
  ) {
    guard !didPositionInitialHistory, let newest = messages.last else { return }
    didPositionInitialHistory = true
    Task { @MainActor in
      await Task.yield()
      proxy.scrollTo(newest.id, anchor: .bottom)
    }
  }
}

#Preview("Messages") {
  let dependencies = try! AppDependencies(
    database: PersistenceContainer(inMemory: true),
    apiClient: APIClient(configuration: .localDevelopment),
    webSocket: WebSocketClient(configuration: .localDevelopment)
  )

  NavigationStack {
    ChatView(
      viewModel: ChatViewModel(
        peerId: UUID(),
        conversations: dependencies.conversations,
        messages: dependencies.messages,
        messaging: dependencies.messaging
      ),
      onCancel: { }
    )
  }
  .preferredColorScheme(.light)
}
// MARK: - AI Generated - End
