import SwiftUI

nonisolated enum ConnectionStatusState: Equatable, Sendable {
  case connected
  case connecting
  case offline(String)
}

struct ConnectionStatusView: View {
  private enum Constants {
    static let cornerRadius: CGFloat = 16
  }

  let state: ConnectionStatusState
  let retryAction: () -> Void

  @ViewBuilder
  var body: some View {
    switch state {
    case .connected:
      EmptyView()
    case .connecting:
      banner(
        icon: "arrow.triangle.2.circlepath",
        message: "Connecting...",
        allowsRetry: false
      )
    case .offline(let message):
      banner(icon: "wifi.slash", message: message, allowsRetry: true)
    }
  }

  private func banner(
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
        Button("Retry", action: retryAction)
          .font(.body.weight(.semibold))
          .foregroundStyle(Tokens.Colors.primary)
      }
    }
    .foregroundStyle(Tokens.Colors.textPrimary)
    .padding(Tokens.Spacing.medium.value)
    .background(Tokens.Colors.secondary)
    .clipShape(RoundedRectangle(cornerRadius: Constants.cornerRadius))
  }
}

#Preview("Connection status") {
  VStack(spacing: Tokens.Spacing.medium.value) {
    ConnectionStatusView(state: .connecting, retryAction: { })
    ConnectionStatusView(
      state: .offline("You are offline. Messages will be sent when you reconnect."),
      retryAction: { }
    )
  }
  .padding(Tokens.Spacing.medium.value)
  .background(Tokens.Colors.background)
}
