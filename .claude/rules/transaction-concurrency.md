# Transaction & Concurrency Rules

## Transaction Annotations

### Read Operations
```kotlin
@Transactional(readOnly = true)
fun getFeedDetail(feedId: Long): FeedDetailResponse {
    // Query operations only
}
```

### Write Operations
```kotlin
@Transactional
fun uploadFeed(userId: Long, request: FeedUploadRequest) {
    // Create/Update/Delete operations
}
```

## Transaction Placement

- Place `@Transactional` on **UseCase** methods
- Domain Services should NOT have `@Transactional`
- Let UseCase manage transaction boundaries

```kotlin
@Service
class FeedUsecase(
    private val feedSaveService: FeedSaveService,
    private val mediaSaveService: MediaSaveService,
    private val feedMapper: FeedMapper
) {
    @Transactional
    fun uploadFeed(userId: Long, request: FeedUploadRequest) {
        val feed = feedMapper.toFeed(user, request.description)
        feedSaveService.save(feed)
        mediaSaveService.saveAll(feed, request.media)
    }
}
```

## Pessimistic Locking

For resources that need concurrent access control:

```kotlin
interface FeedRepository : JpaRepository<Feed, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT f FROM Feed f WHERE f.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): Feed?
}
```

## When to Use Locking

| Scenario | Lock Type |
|----------|-----------|
| Counter updates (reaction count) | PESSIMISTIC_WRITE |
| Concurrent modifications | PESSIMISTIC_WRITE |
| Read-heavy, write-rare | OPTIMISTIC (version field) |

## Lock Timeout Handling

```kotlin
@Service
class ReactionUsecase(
    private val feedRepository: FeedRepository
) {
    @Transactional
    fun react(userId: Long, feedId: Long) {
        try {
            val feed = feedRepository.findByIdWithLock(feedId)
                ?: throw FeedNotFoundException()
            // process reaction
        } catch (e: PessimisticLockingFailureException) {
            throw ResourceLockedException()
        }
    }
}
```

## Optimistic Locking

Add version field to entity:

```kotlin
@Entity
class Feed(
    @Version
    val version: Long = 0
) : BaseEntity()
```

## Transaction Propagation

Default propagation is `REQUIRED`. Use others when needed:

```kotlin
// New transaction (for audit logs, etc.)
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun logAction(action: String) { }

// No transaction
@Transactional(propagation = Propagation.NOT_SUPPORTED)
fun nonTransactionalOperation() { }
```

## Transaction Isolation

Default is database default. Adjust for specific needs:

```kotlin
@Transactional(isolation = Isolation.SERIALIZABLE)
fun criticalOperation() { }
```

## Async Operations

For async operations, transaction context is NOT propagated:

```kotlin
@Async
@Transactional
fun asyncOperation() {
    // New transaction in async thread
}
```

## Best Practices

1. **Keep transactions short** - Don't do I/O operations inside transactions
2. **Avoid nested transactions** - Can cause unexpected behavior
3. **Lock ordering** - Always acquire locks in same order to prevent deadlocks
4. **Timeout configuration** - Always set lock timeouts
5. **Handle lock exceptions** - Convert to user-friendly errors