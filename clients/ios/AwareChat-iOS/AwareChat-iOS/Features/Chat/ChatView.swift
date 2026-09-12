// MARK: - AI Generated - Start
import SwiftUI

struct ChatView: View {
  private enum Constants {
    static let maximumContentWidth: CGFloat = 700
    static let composerCornerRadius: CGFloat = 24
    static let sendButtonSize: CGFloat = 44
    static let statusCornerRadius: CGFloat = 16
  }

  @State private var viewModel: ChatViewModel
  @FocusState private var isComposerFocused: Bool
  let onCancel: () -> Void

  init(viewModel: ChatViewModel, onCancel: @escaping () -> Void) {
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
        conversation(messages)
      }
    }
    .navigationTitle(viewModel.peerName)
    .navigationBarTitleDisplayMode(.inline)
    .background(Tokens.Colors.background)
    .task { await viewModel.load() }
    .onDisappear { viewModel.stopObserving() }
  }

  private func conversation(_ messages: [LocalMessage]) -> some View {
    ScrollViewReader { proxy in
      ScrollView {
        LazyVStack(spacing: Tokens.Spacing.medium.value) {
          connectionStatus
          generalIssue

          ForEach(messages) { message in
            messageRow(message)
            .id(message.id)
          }
        }
        .padding(.horizontal, Tokens.Spacing.medium.value)
        .padding(.vertical, Tokens.Spacing.medium.value)
        .frame(maxWidth: Constants.maximumContentWidth)
        .frame(maxWidth: .infinity)
      }
      .scrollDismissesKeyboard(.interactively)
      .onChange(of: messages.map(\.id), initial: true) { _, ids in
        guard let lastId = ids.last else { return }
        proxy.scrollTo(lastId, anchor: .bottom)
      }
      .onChange(of: isComposerFocused) { _, isFocused in
        guard isFocused, let lastId = messages.last?.id else { return }
        withAnimation { proxy.scrollTo(lastId, anchor: .bottom) }
      }
    }
    .safeAreaInset(edge: .bottom, spacing: 0) {
      composer
    }
    .background(Tokens.Colors.background.ignoresSafeArea())
  }

  @ViewBuilder
  private var generalIssue: some View {
    if let issue = viewModel.latestIssue, issue.messageId == nil {
      issueBanner(issue.message)
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

      if message.direction == .outgoing,
         message.state != .sent,
         viewModel.latestIssue?.messageId == message.id,
         let issue = viewModel.latestIssue {
        BodyText(issue.message)
          .foregroundStyle(Tokens.Colors.red)
          .accessibilityLabel("Error: \(issue.message)")
      }
    }
  }

  private func issueBanner(_ message: String) -> some View {
    HStack(alignment: .firstTextBaseline, spacing: Tokens.Spacing.small.value) {
      BodyText(message)
        .foregroundStyle(Tokens.Colors.textPrimary)
        .frame(maxWidth: .infinity, alignment: .leading)
      Button("Dismiss") { viewModel.dismissLatestIssue() }
        .font(.body.weight(.semibold))
        .foregroundStyle(Tokens.Colors.primary)
    }
    .padding(Tokens.Spacing.medium.value)
    .background(Tokens.Colors.secondary)
    .clipShape(RoundedRectangle(cornerRadius: Constants.statusCornerRadius))
  }

  @ViewBuilder
  private var connectionStatus: some View {
    switch viewModel.connectionState {
    case .connected:
      EmptyView()
    case .connecting:
      statusBanner(
        icon: "arrow.triangle.2.circlepath",
        message: "Connecting...",
        allowsRetry: false
      )
    case .offline(let message):
      statusBanner(icon: "wifi.slash", message: message, allowsRetry: true)
    }
  }

  private func statusBanner(
    icon: String,
    message: String,
    allowsRetry: Bool
  ) -> some View {
    HStack(spacing: Tokens.Spacing.small.value) {
      Image(systemName: icon)
        .accessibilityHidden(true)
      BodyText(message)
        .frame(maxWidth: .infinity, alignment: .leading)
      if allowsRetry {
        Button("Retry") { viewModel.retryConnection() }
          .font(.body.weight(.semibold))
          .foregroundStyle(Tokens.Colors.primary)
      }
    }
    .foregroundStyle(Tokens.Colors.textPrimary)
    .padding(Tokens.Spacing.medium.value)
    .background(Tokens.Colors.secondary)
    .clipShape(RoundedRectangle(cornerRadius: Constants.statusCornerRadius))
  }

  private var composer: some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall.value) {
      if let message = viewModel.validationMessage ?? viewModel.sendErrorMessage {
        BodyText(message)
          .foregroundStyle(Tokens.Colors.red)
          .accessibilityLabel("Error: \(message)")
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
        .submitLabel(.send)
        .focused($isComposerFocused)
        .onSubmit(send)
        .padding(.horizontal, Tokens.Spacing.medium.value)
        .padding(.vertical, Tokens.Spacing.small.value)
        .background(Tokens.Colors.secondary)
        .clipShape(RoundedRectangle(cornerRadius: Constants.composerCornerRadius))
        .accessibilityLabel("Message")

        Button(action: send) {
          Image(systemName: "paperplane.fill")
            .font(.title2)
            .foregroundStyle(Tokens.Colors.background)
            .frame(
              width: Constants.sendButtonSize,
              height: Constants.sendButtonSize
            )
            .background(Tokens.Colors.primary)
            .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!viewModel.canSend)
        .opacity(viewModel.canSend ? 1 : 0.5)
        .accessibilityLabel("Send message")
      }
    }
    .padding(.horizontal, Tokens.Spacing.medium.value)
    .padding(.vertical, Tokens.Spacing.small.value)
    .frame(maxWidth: Constants.maximumContentWidth)
    .frame(maxWidth: .infinity)
    .background(Tokens.Colors.background)
    .overlay(alignment: .top) {
      Divider().overlay(Tokens.Colors.divider)
    }
  }

  private func ackState(for message: LocalMessage) -> ACKMessageState {
    guard message.direction == .outgoing else { return .sending }
    switch message.state {
    case .sent:
      return .sent
    case .failed:
      return .failed
    case .pendingToSend, .sending, .none:
      return .sending
    }
  }

  private func send() {
    Task { await viewModel.send() }
  }
}
// MARK: - AI Generated - End
