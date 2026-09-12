// MARK: - AI Generated - Start
import Observation
import SwiftUI

@MainActor
@Observable
private final class AppRootViewModel {
  enum State {
    case loading
    case ready(IdentificationViewModel)
    case error(String)
  }

  private(set) var state = State.loading
  let coordinator = AppCoordinator()

  private var dependencies: AppDependencies?
  private var didStart = false

  func start() {
    guard !didStart else { return }
    didStart = true
    state = .loading

    do {
      let dependencies = try AppDependencies.live()
      self.dependencies = dependencies
      let coordinator = coordinator
      let identification = IdentificationViewModel(
        users: dependencies.users,
        messaging: dependencies.messaging,
        onRegistrationCompleted: { [weak coordinator] in
          coordinator?.didCompleteRegistration()
        }
      )
      state = .ready(identification)
      coordinator.start(hasCompletedRegistration: false)
    } catch {
      state = .error(error.localizedDescription)
    }
  }

  func retryStartup() {
    didStart = false
    start()
  }
}

struct ContentView: View {
  @State private var viewModel = AppRootViewModel()

  var body: some View {
    @Bindable var router = viewModel.coordinator.router

    Group {
      switch viewModel.state {
      case .loading:
        LoadingScreen()
      case .error(let message):
        ErrorScreen(
          message: message,
          retryAction: { viewModel.retryStartup() },
          showsCancel: false
        )
      case .ready(let identification):
        NavigationStack(path: $router.path) {
          rootContent(identification: identification)
        }
      }
    }
    .task { viewModel.start() }
  }

  @ViewBuilder
  private func rootContent(identification: IdentificationViewModel) -> some View {
    switch viewModel.coordinator.flow {
    case .loading:
      LoadingScreen()
    case .identification:
      IdentificationView(viewModel: identification)
    case .conversations:
      conversationsPlaceholder
    }
  }

  private var conversationsPlaceholder: some View {
    VStack(alignment: .leading) {
      LargeTitleText("Chat", weight: .bold)
        .foregroundStyle(Tokens.Colors.textPrimary)
      Spacer()
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .padding(Tokens.Spacing.large.value)
    .background(Tokens.Colors.background.ignoresSafeArea())
  }
}

#Preview {
  ContentView()
    .preferredColorScheme(.light)
}
// MARK: - AI Generated - End
