// MARK: - AI Generated - Start
import Foundation
import Testing
@testable import AwareChat_iOS

@MainActor
@Suite("User list ViewModel")
struct UserListViewModelTests {
  @Test
  func localConversationsRenderWhileDiscoveryIsStillLoading() async throws {
    let conversation = try makeConversation(
      peerId: UserListIDs.bob,
      peerName: "Bob",
      text: "Hello",
      state: .sent
    )
    let harness = UserListHarness(
      conversations: [conversation],
      apiResponse: .suspended
    )

    let load = Task { await harness.viewModel.load() }
    defer { load.cancel() }

    try await eventually {
      harness.viewModel.screenState == .ready([conversation])
        && harness.viewModel.discoveryState == .loading
    }
  }

  @Test
  func discoveryFiltersByIdentityAndConversationPeerIds() async throws {
    let conversation = try makeConversation(
      peerId: UserListIDs.bob,
      peerName: "Bob",
      text: "Hello",
      state: .sent
    )
    let sameNameOne = try UserDTO(userId: UserListIDs.carol, name: "Alex")
    let sameNameTwo = try UserDTO(userId: UserListIDs.dave, name: "Alex")
    let harness = UserListHarness(
      conversations: [conversation],
      apiResponse: .success([
        try UserDTO(userId: UserListIDs.current, name: "Me"),
        try UserDTO(userId: UserListIDs.bob, name: "Bob"),
        sameNameOne,
        sameNameTwo,
      ])
    )

    await harness.viewModel.load()

    try await eventually {
      harness.viewModel.discoveryState == .available([
        DiscoveredUser(id: sameNameOne.userId, name: sameNameOne.name),
        DiscoveredUser(id: sameNameTwo.userId, name: sameNameTwo.name),
      ])
    }
    #expect(harness.users.upsertedUsers.count == 4)
  }

  @Test
  func anExhaustedDiscoveryListUsesTheSpecifiedEmptyState() async throws {
    let conversation = try makeConversation(
      peerId: UserListIDs.bob,
      peerName: "Bob",
      text: "Hello",
      state: .sent
    )
    let harness = UserListHarness(
      conversations: [conversation],
      apiResponse: .success([
        try UserDTO(userId: UserListIDs.current, name: "Me"),
        try UserDTO(userId: UserListIDs.bob, name: "Bob"),
      ])
    )

    await harness.viewModel.load()

    try await eventually {
      harness.viewModel.discoveryState == .empty
        && harness.viewModel.screenState == .ready([conversation])
    }
  }

  @Test
  func discoveryNetworkFailureDoesNotReplaceLocalContent() async throws {
    let harness = UserListHarness(apiResponse: .failure(.transportFailure))

    await harness.viewModel.load()

    try await eventually {
      harness.viewModel.screenState == .ready([])
        && harness.viewModel.discoveryState
          == .error(NetworkError.transportFailure.localizedDescription)
    }
  }

  @Test
  func discoveryPersistenceFailureDoesNotReplaceLocalContent() async throws {
    let harness = UserListHarness(
      apiResponse: .success([try UserDTO(userId: UserListIDs.carol, name: "Carol")])
    )
    harness.users.upsertError = .writeFailed

    await harness.viewModel.load()

    try await eventually {
      harness.viewModel.screenState == .ready([])
        && harness.viewModel.discoveryState
          == .error(PersistenceError.writeFailed.localizedDescription)
    }
  }

  @Test
  func selectingADiscoveredUserKeepsThemDiscoverableUntilTheFirstMessage() async throws {
    let discovered = try UserDTO(userId: UserListIDs.carol, name: "Carol")
    let harness = UserListHarness(apiResponse: .success([discovered]))
    harness.conversations.peerNames[discovered.userId] = discovered.name
    await harness.viewModel.load()

    let destination = harness.viewModel.selectUser(discovered.userId)

    #expect(destination == discovered.userId)
    #expect(harness.conversations.creationRequests == [discovered.userId])
    try await eventually {
      guard case .ready(let conversations) = harness.viewModel.screenState else { return false }
      return conversations.isEmpty
        && harness.viewModel.discoveryState == .available([
          DiscoveredUser(id: discovered.userId, name: discovered.name)
        ])
    }

    let conversationWithMessage = try makeConversation(
      peerId: discovered.userId,
      peerName: discovered.name,
      text: "Hello",
      state: .pendingToSend
    )
    harness.conversations.emit([conversationWithMessage])

    try await eventually {
      harness.viewModel.screenState == .ready([conversationWithMessage])
        && harness.viewModel.discoveryState == .empty
    }
  }

  @Test
  func conversationCreationFailureStaysContextualAndDoesNotNavigate() async throws {
    let discovered = try UserDTO(userId: UserListIDs.carol, name: "Carol")
    let harness = UserListHarness(apiResponse: .success([discovered]))
    harness.conversations.creationError = .writeFailed
    await harness.viewModel.load()

    let destination = harness.viewModel.selectUser(discovered.userId)

    #expect(destination == nil)
    #expect(
      harness.viewModel.selectionError == ConversationSelectionError(
        userId: discovered.userId,
        message: PersistenceError.writeFailed.localizedDescription
      )
    )
    try await eventually { harness.viewModel.screenState == .ready([]) }
  }

  @Test
  func localObservationFailureCanBeRetried() async throws {
    let harness = UserListHarness(apiResponse: .success([]))
    harness.conversations.observationError = .readFailed

    await harness.viewModel.load()
    try await eventually {
      harness.viewModel.screenState == .error(PersistenceError.readFailed.localizedDescription)
    }

    harness.conversations.observationError = nil
    harness.viewModel.retryLocalLoad()

    try await eventually { harness.viewModel.screenState == .ready([]) }
  }

  @Test
  func connectionFailureAndRetryStayIndependentFromContent() async throws {
    let harness = UserListHarness(apiResponse: .success([]))
    await harness.viewModel.load()
    await harness.messaging.emit(.connectionFailure(.network(.notConnected)))

    try await eventually {
      harness.viewModel.connectionState == .offline(
        MessagingFailure.network(.notConnected).localizedDescription
      )
    }
    #expect(harness.viewModel.screenState == .ready([]))

    harness.viewModel.retryConnection()

    try await eventually {
      let stopCount = await harness.messaging.stopCount
      let startCount = await harness.messaging.startCount
      return stopCount == 1 && startCount == 1
    }
    #expect(harness.viewModel.screenState == .ready([]))
  }

  @Test
  func committedConversationUpdatesRefreshTheLatestMessage() async throws {
    let initial = try makeConversation(peerId: UserListIDs.bob, peerName: "Bob")
    let updated = try makeConversation(
      peerId: UserListIDs.bob,
      peerName: "Bob",
      text: "See you soon",
      state: .sent
    )
    let harness = UserListHarness(
      conversations: [initial],
      apiResponse: .success([])
    )
    await harness.viewModel.load()

    harness.conversations.emit([updated])

    try await eventually { harness.viewModel.screenState == .ready([updated]) }
  }
}

@MainActor
private final class UserListHarness {
  let users = UserListFakeUserRepository()
  let conversations: UserListFakeConversationRepository
  let apiClient: UserListFakeAPIClient
  let messaging = UserListFakeMessagingService()
  lazy var viewModel = UserListViewModel(
    users: users,
    conversations: conversations,
    apiClient: apiClient,
    messaging: messaging
  )

  init(
    conversations: [LocalConversation] = [],
    apiResponse: UserListFakeAPIClient.Response
  ) {
    self.conversations = UserListFakeConversationRepository(conversations: conversations)
    apiClient = UserListFakeAPIClient(response: apiResponse)
  }
}

@MainActor
private final class UserListFakeUserRepository: UserLocalRepository {
  var current = LocalUser(
    userId: UserListIDs.current,
    name: "Me",
    isCurrent: true,
    registrationCompleted: true
  )
  var readError: PersistenceError?
  var upsertError: PersistenceError?
  private(set) var upsertedUsers = [UserDTO]()

  func currentUser() throws -> LocalUser? {
    if let readError { throw readError }
    return current
  }

  func user(id: UUID) throws -> LocalUser? {
    current.userId == id ? current : nil
  }

  func saveIdentity(name: String) throws -> LocalUser { current }

  func completeRegistration(acceptedUser: UserDTO) throws { }

  func upsertKnownUsers(_ users: [UserDTO]) throws {
    if let upsertError { throw upsertError }
    upsertedUsers = users
  }

  func observeCurrentUser() -> AsyncThrowingStream<LocalUser?, any Error> {
    AsyncThrowingStream { continuation in
      continuation.yield(current)
      continuation.finish()
    }
  }
}

@MainActor
private final class UserListFakeConversationRepository: ConversationLocalRepository {
  var observationError: PersistenceError?
  var creationError: PersistenceError?
  var peerNames = [UUID: String]()
  private(set) var creationRequests = [UUID]()
  private var snapshot: [LocalConversation]
  private var observers = [AsyncThrowingStream<[LocalConversation], any Error>.Continuation]()

  init(conversations: [LocalConversation]) {
    snapshot = conversations
  }

  func conversations() throws -> [LocalConversation] { snapshot }

  func conversation(id: String) throws -> LocalConversation? {
    snapshot.first { $0.id == id }
  }

  func getOrCreate(with userId: UUID) throws -> LocalConversation {
    creationRequests.append(userId)
    if let creationError { throw creationError }
    if let existing = snapshot.first(where: { $0.peer.userId == userId }) { return existing }

    let conversation = LocalConversation(
      conversationId: try WireValidation.conversationID(
        first: UserListIDs.current,
        second: userId
      ),
      peer: LocalUser(
        userId: userId,
        name: peerNames[userId],
        isCurrent: false,
        registrationCompleted: false
      ),
      latestMessage: nil
    )
    snapshot.append(conversation)
    emit(snapshot)
    return conversation
  }

  func observeConversations() -> AsyncThrowingStream<[LocalConversation], any Error> {
    AsyncThrowingStream { continuation in
      if let observationError {
        continuation.finish(throwing: observationError)
      } else {
        observers.append(continuation)
        continuation.yield(snapshot)
      }
    }
  }

  func emit(_ conversations: [LocalConversation]) {
    snapshot = conversations
    for observer in observers { observer.yield(conversations) }
  }
}

private actor UserListFakeAPIClient: APIClientProtocol {
  nonisolated enum Response: Sendable {
    case success([UserDTO])
    case failure(NetworkError)
    case suspended
  }

  let response: Response

  init(response: Response) {
    self.response = response
  }

  func health() throws -> HealthResponse { throw NetworkError.invalidHTTPResponse }

  func users() async throws -> [UserDTO] {
    switch response {
    case .success(let users):
      return users
    case .failure(let error):
      throw error
    case .suspended:
      try await Task.sleep(for: .seconds(60))
      return []
    }
  }
}

private actor UserListFakeMessagingService: MessagingServiceProtocol {
  private var state = MessagingConnectionState.disconnected
  private var observers = [AsyncStream<MessagingConnectionState>.Continuation]()
  private(set) var startCount = 0
  private(set) var stopCount = 0

  func start() {
    startCount += 1
    setState(.connecting)
  }

  func stop() {
    stopCount += 1
    setState(.disconnected)
  }

  func sendMessage(text: String, receiverId: UUID) throws -> LocalMessage {
    throw PersistenceError.missingIdentity
  }

  func connectionState() -> MessagingConnectionState { state }

  func observeConnectionState() -> AsyncStream<MessagingConnectionState> {
    AsyncStream { continuation in
      observers.append(continuation)
      continuation.yield(state)
    }
  }

  func observeIssues() -> AsyncStream<MessagingIssue> {
    AsyncStream { $0.finish() }
  }

  func emit(_ state: MessagingConnectionState) {
    setState(state)
  }

  private func setState(_ newState: MessagingConnectionState) {
    state = newState
    for observer in observers { observer.yield(newState) }
  }
}

private enum UserListIDs {
  static let current = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!
  static let bob = UUID(uuidString: "22222222-2222-4222-8222-222222222222")!
  static let carol = UUID(uuidString: "33333333-3333-4333-8333-333333333333")!
  static let dave = UUID(uuidString: "44444444-4444-4444-8444-444444444444")!
}

private func makeConversation(
  peerId: UUID,
  peerName: String,
  text: String? = nil,
  state: MessageState? = nil
) throws -> LocalConversation {
  let message = try text.map {
    LocalMessage(
      message: try MessageDTO(
        messageId: UUID(uuidString: "55555555-5555-4555-8555-555555555555")!,
        text: $0,
        senderId: UserListIDs.current,
        receiverId: peerId,
        clientCreatedAt: Date(timeIntervalSince1970: 1_700_000_000),
        clientSequence: 1
      ),
      direction: .outgoing,
      state: state,
      receivedAt: nil
    )
  }
  return LocalConversation(
    conversationId: try WireValidation.conversationID(first: UserListIDs.current, second: peerId),
    peer: LocalUser(
      userId: peerId,
      name: peerName,
      isCurrent: false,
      registrationCompleted: false
    ),
    latestMessage: message
  )
}
// MARK: - AI Generated - End
