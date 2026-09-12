// MARK: - AI Generated - Start
import SwiftUI

struct UserListView: View {
  @State private var viewModel: UserListViewModel
  let onSelectUser: (UUID) -> Void

  init(
    viewModel: UserListViewModel,
    onSelectUser: @escaping (UUID) -> Void
  ) {
    _viewModel = State(initialValue: viewModel)
    self.onSelectUser = onSelectUser
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
          showsCancel: false
        )
      case .ready(let conversations):
        content(conversations: conversations)
      }
    }
    .task { await viewModel.load() }
  }

  private func content(conversations: [LocalConversation]) -> some View {
    ScrollView {
      LazyVStack(alignment: .leading, spacing: Tokens.Spacing.large.value) {
        ConnectionStatusView(
          state: viewModel.connectionState,
          retryAction: { viewModel.retryConnection() }
        )
        conversationSection(conversations)
        peopleSection
      }
      .padding(.horizontal, Tokens.Spacing.medium.value)
      .padding(.vertical, Tokens.Spacing.large.value)
      .frame(maxWidth: Constants.maximumContentWidth)
      .frame(maxWidth: .infinity)
    }
    .refreshable { await viewModel.refreshUsers() }
    .background(Tokens.Colors.background.ignoresSafeArea())
  }

  private func conversationSection(_ conversations: [LocalConversation]) -> some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
      sectionTitle("Chat")

      if !conversations.isEmpty {
        rowContainer {
          ForEach(Array(conversations.enumerated()), id: \.element.id) { index, conversation in
            conversationRow(conversation)
            if index < conversations.count - 1 { Divider().overlay(Tokens.Colors.divider) }
          }
        }
      }
    }
  }

  private var peopleSection: some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
      sectionTitle("People on server")

      switch viewModel.discoveryState {
      case .idle, .loading:
        HStack {
          Spacer()
          ProgressView()
            .accessibilityLabel("Loading people on server")
          Spacer()
        }
        .padding(Tokens.Spacing.large.value)
      case .empty:
        rowContainer {
          BodyText("No other users available right now")
            .foregroundStyle(Tokens.Colors.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Tokens.Spacing.medium.value)
        }
      case .error(let message):
        rowContainer {
          VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
            BodyText(message)
              .foregroundStyle(Tokens.Colors.textPrimary)
            Button("Retry") {
              Task { await viewModel.refreshUsers() }
            }
            .font(.body.weight(.semibold))
            .foregroundStyle(Tokens.Colors.primary)
          }
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding(Tokens.Spacing.medium.value)
        }
      case .available(let users):
        rowContainer {
          ForEach(Array(users.enumerated()), id: \.element.id) { index, user in
            personRow(user)
            if index < users.count - 1 { Divider().overlay(Tokens.Colors.divider) }
          }
        }
      }
    }
  }

  private func conversationRow(_ conversation: LocalConversation) -> some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall.value) {
      Button {
        navigate(to: conversation.peer.userId)
      } label: {
        HStack(spacing: Tokens.Spacing.medium.value) {
          VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall.value) {
            BodyText(conversation.peer.name ?? "Unknown user", weight: .semibold)
              .foregroundStyle(Tokens.Colors.textPrimary)
              .frame(maxWidth: .infinity, alignment: .leading)

            if let latestMessage = conversation.latestMessage {
              HStack(spacing: Tokens.Spacing.xSmall.value) {
                messageStateIcon(latestMessage.state)
                BodyText(latestMessage.message.text)
                  .foregroundStyle(Tokens.Colors.textSecondary)
                  .lineLimit(1)
              }
            }
          }

          if let date = conversation.latestMessage?.displayTimestamp {
            BodyText(date.formatted(.dateTime.day().month(.abbreviated)))
              .foregroundStyle(Tokens.Colors.textSecondary)
              .lineLimit(1)
          }

          chevron
        }
        .contentShape(Rectangle())
        .padding(.vertical, Tokens.Spacing.medium.value)
      }
      .buttonStyle(.plain)
      .disabled(viewModel.selectedUserId != nil)

      selectionFailure(for: conversation.peer.userId)
    }
    .padding(.horizontal, Tokens.Spacing.medium.value)
  }

  private func personRow(_ user: DiscoveredUser) -> some View {
    VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall.value) {
      Button {
        navigate(to: user.id)
      } label: {
        HStack(spacing: Tokens.Spacing.medium.value) {
          BodyText(user.name, weight: .semibold)
            .foregroundStyle(Tokens.Colors.textPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)
          chevron
        }
        .contentShape(Rectangle())
        .padding(.vertical, Tokens.Spacing.medium.value)
      }
      .buttonStyle(.plain)
      .disabled(viewModel.selectedUserId != nil)

      selectionFailure(for: user.id)
    }
    .padding(.horizontal, Tokens.Spacing.medium.value)
  }

  @ViewBuilder
  private func selectionFailure(for userId: UUID) -> some View {
    if let error = viewModel.selectionError, error.userId == userId {
      HStack(alignment: .firstTextBaseline, spacing: Tokens.Spacing.small.value) {
        BodyText(error.message)
          .foregroundStyle(Tokens.Colors.textPrimary)
        Button("Retry") { navigate(to: userId) }
          .font(.body.weight(.semibold))
          .foregroundStyle(Tokens.Colors.primary)
      }
      .padding(.bottom, Tokens.Spacing.small.value)
    }
  }

  @ViewBuilder
  private func messageStateIcon(_ state: MessageState?) -> some View {
    switch state {
    case .pendingToSend, .sending:
      Image(systemName: "clock")
        .foregroundStyle(Tokens.Colors.textSecondary)
        .accessibilityLabel("Pending message")
    case .failed:
      Image(systemName: "exclamationmark.circle.fill")
        .foregroundStyle(Tokens.Colors.red)
        .accessibilityLabel("Failed message")
    case .sent, .none:
      EmptyView()
    }
  }

  private var chevron: some View {
    Image(systemName: "chevron.right")
      .font(.body.weight(.semibold))
      .foregroundStyle(Tokens.Colors.textSecondary)
      .accessibilityHidden(true)
  }

  private func sectionTitle(_ title: String) -> some View {
    TitleText(title, weight: .bold)
      .foregroundStyle(Tokens.Colors.textPrimary)
  }

  private func rowContainer<Content: View>(
    @ViewBuilder content: () -> Content
  ) -> some View {
    VStack(spacing: 0, content: content)
      .background(Tokens.Colors.secondary)
      .clipShape(RoundedRectangle(cornerRadius: Constants.containerRadius))
  }

  private func navigate(to userId: UUID) {
    if let destination = viewModel.selectUser(userId) {
      onSelectUser(destination)
    }
  }

  private enum Constants {
    static let containerRadius: CGFloat = 20
    static let maximumContentWidth: CGFloat = 700
  }
}
// MARK: - AI Generated - End
