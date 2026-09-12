// MARK: - AI Generated - Start
import SwiftUI

struct IdentificationView: View {
  @State private var viewModel: IdentificationViewModel
  @FocusState private var isNameFocused: Bool

  init(viewModel: IdentificationViewModel) {
    _viewModel = State(initialValue: viewModel)
  }

  var body: some View {
    Group {
      switch viewModel.state {
      case .loading:
        LoadingScreen()
      case .form:
        form
      case .error(let presentation):
        errorScreen(presentation)
      }
    }
    .background(Tokens.Colors.background)
    .task { await viewModel.load() }
  }

  private var form: some View {
    GeometryReader { geometry in
      ScrollView {
        VStack(alignment: .leading, spacing: Tokens.Spacing.medium.value) {
          VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
            LargeTitleText("Sign up", weight: .bold)
              .foregroundStyle(Tokens.Colors.textPrimary)

            Text("Create your chat account")
              .font(.title3)
              .foregroundStyle(Tokens.Colors.textSecondary)
          }

          VStack(alignment: .leading, spacing: Tokens.Spacing.small.value) {
            HStack(spacing: Tokens.Spacing.medium.value) {
              Image(systemName: "person")
                .font(.title2)
                .foregroundStyle(Tokens.Colors.textSecondary)
                .accessibilityHidden(true)

              TextField(
                "User name",
                text: $viewModel.name,
                prompt: Text("User name").foregroundColor(Tokens.Colors.textSecondary)
              )
                .font(.title3)
                .foregroundStyle(Tokens.Colors.textPrimary)
                .textContentType(.name)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .focused($isNameFocused)
                .onSubmit(viewModel.confirm)
                .accessibilityLabel("User name")
            }
            .padding(.horizontal, Tokens.Spacing.medium.value)
            .frame(minHeight: Constants.fieldHeight)
            .overlay {
              RoundedRectangle(cornerRadius: Constants.fieldCornerRadius)
                .stroke(Tokens.Colors.divider, lineWidth: Constants.borderWidth)
            }

            if let validationMessage = viewModel.validationMessage {
              BodyText(validationMessage)
                .foregroundStyle(Tokens.Colors.textPrimary)
                .accessibilityLabel("Error: \(validationMessage)")
            }
          }
          .padding(.top, Tokens.Spacing.large.value)

          LargeButton("Confirm", style: .primary) {
            isNameFocused = false
            viewModel.confirm()
          }
          .accessibilityHint("Registers this user name on the chat server")
          .padding(.top, Tokens.Spacing.small.value)
        }
        .padding(.horizontal, Tokens.Spacing.large.value)
        .frame(maxWidth: Constants.maximumFormWidth)
        .frame(maxWidth: .infinity)
        .frame(minHeight: geometry.size.height, alignment: .center)
      }
      .scrollDismissesKeyboard(.interactively)
    }
    .background(Tokens.Colors.background.ignoresSafeArea())
  }

  private func errorScreen(_ presentation: IdentificationErrorPresentation) -> some View {
    ErrorScreen(
      message: presentation.message,
      retryAction: presentation.allowsRetry ? { viewModel.retry() } : nil,
      cancelAction: presentation.allowsCancel ? { viewModel.cancel() } : nil,
      showsCancel: presentation.allowsCancel
    )
  }

  private enum Constants {
    static let fieldHeight: CGFloat = 60
    static let fieldCornerRadius: CGFloat = 14
    static let borderWidth: CGFloat = 1
    static let maximumFormWidth: CGFloat = 560
  }
}

#Preview("Identification") {
  let dependencies = try! AppDependencies(
    database: PersistenceContainer(inMemory: true),
    apiClient: APIClient(configuration: .localDevelopment),
    webSocket: WebSocketClient(configuration: .localDevelopment)
  )

  IdentificationView(
    viewModel: IdentificationViewModel(
      users: dependencies.users,
      messaging: dependencies.messaging,
      onRegistrationCompleted: { }
    )
  )
  .preferredColorScheme(.light)
}
// MARK: - AI Generated - End
