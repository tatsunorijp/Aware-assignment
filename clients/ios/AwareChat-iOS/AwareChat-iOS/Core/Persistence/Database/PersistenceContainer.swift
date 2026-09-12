import Foundation
import SwiftData

/// Owns the only writing context. Repository operations never suspend mid-transaction.
@MainActor
final class PersistenceContainer {
  private let container: ModelContainer
  let context: ModelContext
  private let save: (ModelContext) throws -> Void
  private var observers: [UUID: () -> Void] = [:]

  init(
    inMemory: Bool = false,
    storeURL: URL? = nil,
    save: @escaping (ModelContext) throws -> Void = { try $0.save() }
  ) throws {
    let schema = Schema([UserRecord.self, ConversationRecord.self, MessageRecord.self])
    let configuration: ModelConfiguration
    if let storeURL {
      configuration = ModelConfiguration(schema: schema, url: storeURL, cloudKitDatabase: .none)
    } else {
      configuration = ModelConfiguration(
        schema: schema, isStoredInMemoryOnly: inMemory, cloudKitDatabase: .none
      )
    }
    do {
      container = try ModelContainer(for: schema, configurations: [configuration])
    } catch {
      throw PersistenceError.readFailed
    }
    context = ModelContext(container)
    context.autosaveEnabled = false
    self.save = save
  }

  func read<T>(_ operation: () throws -> T) throws -> T {
    do { return try operation() }
    catch let error as PersistenceError { throw error }
    catch { throw PersistenceError.readFailed }
  }

  @discardableResult
  func write<T>(_ operation: () throws -> T) throws -> T {
    do {
      let result = try operation()
      if context.hasChanges {
        try save(context)
        // A save hook must commit, never silently leave dirty data behind.
        guard !context.hasChanges else { throw PersistenceError.writeFailed }
        for notify in Array(observers.values) { notify() }
      }
      return result
    } catch {
      context.rollback()
      if let error = error as? PersistenceError { throw error }
      if error is WireModelError { throw PersistenceError.invalidData }
      throw PersistenceError.writeFailed
    }
  }

  /// Every subscription gets a committed initial snapshot and subsequent committed changes.
  func observe<T: Sendable>(
    _ query: @escaping @MainActor () throws -> T
  ) -> AsyncThrowingStream<T, any Error> {
    let id = UUID()
    let (stream, continuation) = AsyncThrowingStream<T, any Error>.makeStream(
      bufferingPolicy: .bufferingNewest(1)
    )
    let publish = {
      do { continuation.yield(try query()) }
      catch { continuation.finish(throwing: error) }
    }
    observers[id] = publish
    continuation.onTermination = { [weak self] _ in
      Task { @MainActor [weak self] in self?.observers.removeValue(forKey: id) }
    }
    publish()
    return stream
  }

  func userRecord(_ id: UUID) throws -> UserRecord? {
    try context.fetch(FetchDescriptor<UserRecord>(predicate: #Predicate { $0.userId == id })).first
  }

  func currentIdentityRecord() throws -> UserRecord? {
    try context.fetch(FetchDescriptor<UserRecord>(predicate: #Predicate { $0.isCurrent })).first
  }

  func ensureUser(_ id: UUID) throws {
    if try userRecord(id) == nil { context.insert(UserRecord(userId: id, name: nil)) }
  }

  func conversationRecord(_ id: String) throws -> ConversationRecord? {
    try context.fetch(FetchDescriptor<ConversationRecord>(
      predicate: #Predicate { $0.conversationId == id }
    )).first
  }

  func ensureConversation(first: UUID, second: UUID) throws -> ConversationRecord {
    let id = try WireValidation.conversationID(first: first, second: second)
    if let record = try conversationRecord(id) { return record }
    try ensureUser(first)
    try ensureUser(second)
    let record = ConversationRecord(conversationId: id, first: first, second: second)
    context.insert(record)
    return record
  }

  func messageRecord(_ id: UUID) throws -> MessageRecord? {
    try context.fetch(FetchDescriptor<MessageRecord>(predicate: #Predicate { $0.messageId == id })).first
  }

  func messageRecords(conversationId: String) throws -> [MessageRecord] {
    try context.fetch(FetchDescriptor<MessageRecord>(
      predicate: #Predicate { $0.conversationId == conversationId },
      sortBy: [SortDescriptor(\MessageRecord.displayTimestamp), SortDescriptor(\MessageRecord.clientSequence)]
    ))
  }
}
