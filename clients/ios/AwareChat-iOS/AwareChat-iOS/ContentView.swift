// MARK: - AI Generated - Start
import Observation
import SwiftUI

@MainActor
@Observable
private final class AppRootViewModel {
  enum State {
    case loading
    case ready(
      identification: IdentificationViewModel,
      userList: UserListViewModel
    )
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
      let userList = UserListViewModel(
        users: dependencies.users,
        conversations: dependencies.conversations,
        apiClient: dependencies.apiClient,
        messaging: dependencies.messaging
      )
      state = .ready(identification: identification, userList: userList)
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
      case .ready(let identification, let userList):
        NavigationStack(path: $router.path) {
          rootContent(identification: identification, userList: userList)
            .navigationDestination(for: AppRoute.self) { route in
              destination(for: route)
            }
        }
      }
    }
    .task { viewModel.start() }
  }

  @ViewBuilder
  private func rootContent(
    identification: IdentificationViewModel,
    userList: UserListViewModel
  ) -> some View {
    switch viewModel.coordinator.flow {
    case .loading:
      LoadingScreen()
    case .identification:
      IdentificationView(viewModel: identification)
    case .conversations:
      UserListView(
        viewModel: userList,
        onSelectUser: { userId in
          viewModel.coordinator.router.showMessages(with: userId)
        }
      )
    }
  }

  @ViewBuilder
  private func destination(for route: AppRoute) -> some View {
    switch route {
    case .messages:
      messagesPlaceholder
    }
  }

  private var messagesPlaceholder: some View {
    VStack(alignment: .leading) {
      LargeTitleText("Messages", weight: .bold)
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
