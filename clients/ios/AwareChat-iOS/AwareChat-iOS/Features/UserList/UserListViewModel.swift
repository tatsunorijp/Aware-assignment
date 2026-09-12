// MARK: - AI Generated - Start
import Foundation
import Observation

nonisolated enum UserListScreenState: Equatable, Sendable {
  case loading
  case ready([LocalConversation])
  case error(String)
}

nonisolated struct DiscoveredUser: Equatable, Identifiable, Sendable {
  let id: UUID
  let name: String
}

nonisolated enum UserDiscoveryState: Equatable, Sendable {
  case idle
  case loading
  case available([DiscoveredUser])
  case empty
  case error(String)
}

nonisolated enum UserListConnectionState: Equatable, Sendable {
  case connected
  case connecting
  case offline(String)
}

nonisolated struct ConversationSelectionError: Equatable, Sendable {
  let userId: UUID
  let message: String
}

@MainActor
@Observable
final class UserListViewModel {
  private(set) var screenState = UserListScreenState.loading
  private(set) var discoveryState = UserDiscoveryState.idle
  private(set) var connectionState = UserListConnectionState.connecting
  private(set) var selectionError: ConversationSelectionError?
  private(set) var selectedUserId: UUID?

  private let users: any UserLocalRepository
  private let conversations: any ConversationLocalRepository
  private let apiClient: any APIClientProtocol
  private let messaging: any MessagingServiceProtocol

  private var localConversations = [LocalConversation]()
  private var remoteUsers: [UserDTO]?
  private var didLoad = false
  private var activeRefresh: UUID?
  private var conversationTask: Task<Void, Never>?
  private var connectionTask: Task<Void, Never>?
  private var connectionRetryTask: Task<Void, Never>?

  init(
    users: any UserLocalRepository,
    conversations: any ConversationLocalRepository,
    apiClient: any APIClientProtocol,
    messaging: any MessagingServiceProtocol
  ) {
    self.users = users
    self.conversations = conversations
    self.apiClient = apiClient
    self.messaging = messaging
  }

  func load() async {
    if !didLoad {
      didLoad = true
      observeConversations()
      observeConnection()
    }
    if remoteUsers == nil {
      await refreshUsers()
    }
  }

  func refreshUsers() async {
    let refresh = UUID()
    activeRefresh = refresh
    discoveryState = .loading

    do {
      let response = try await apiClient.users()
      try Task.checkCancellation()
      guard activeRefresh == refresh else { return }

      try users.upsertKnownUsers(response)
      guard activeRefresh == refresh else { return }
      remoteUsers = response
      activeRefresh = nil
      updateDiscoveryState()
    } catch is CancellationError {
      restoreDiscoveryAfterCancellation(refresh)
    } catch let error as NetworkError where error == .cancelled {
      restoreDiscoveryAfterCancellation(refresh)
    } catch {
      guard activeRefresh == refresh else { return }
      activeRefresh = nil
      discoveryState = .error(error.localizedDescription)
    }
  }

  func selectUser(_ userId: UUID) -> UUID? {
    guard selectedUserId == nil else { return nil }
    selectedUserId = userId
    selectionError = nil
    defer { selectedUserId = nil }

    do {
      let conversation = try conversations.getOrCreate(with: userId)
      return conversation.peer.userId
    } catch {
      selectionError = ConversationSelectionError(
        userId: userId,
        message: error.localizedDescription
      )
      return nil
    }
  }

  func retryLocalLoad() {
    screenState = .loading
    observeConversations()
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

  private func observeConversations() {
    conversationTask?.cancel()
    let conversations = conversations
    conversationTask = Task { [weak self] in
      do {
        for try await snapshot in conversations.observeConversations() {
          try Task.checkCancellation()
          guard let self else { return }
          localConversations = snapshot
          screenState = .ready(snapshot)
          updateDiscoveryState()
        }
      } catch is CancellationError {
        return
      } catch {
        guard let self else { return }
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

  private func updateDiscoveryState() {
    guard let remoteUsers else { return }
    let currentId: UUID?
    do {
      currentId = try users.currentUser()?.userId
    } catch {
      discoveryState = .error(error.localizedDescription)
      return
    }

    let conversationPeerIds = Set(localConversations.map(\.peer.userId))
    let filtered = remoteUsers.compactMap { user -> DiscoveredUser? in
      guard user.userId != currentId, !conversationPeerIds.contains(user.userId) else {
        return nil
      }
      return DiscoveredUser(id: user.userId, name: user.name)
    }
    discoveryState = filtered.isEmpty ? .empty : .available(filtered)
  }

  private func restoreDiscoveryAfterCancellation(_ refresh: UUID) {
    guard activeRefresh == refresh else { return }
    activeRefresh = nil
    if remoteUsers == nil {
      discoveryState = .idle
    } else {
      updateDiscoveryState()
    }
  }

  private static func presentationState(
    _ state: MessagingConnectionState
  ) -> UserListConnectionState {
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
// MARK: - AI Generated - End
