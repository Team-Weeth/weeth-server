# Transaction & Concurrency Rules

## Transaction Placement

- `@Transactional` goes on **UseCase** methods only — Command: `@Transactional`, Query: `@Transactional(readOnly = true)`
- Domain Services must NOT have `@Transactional`; UseCase owns transaction boundaries
- Keep transactions short — no external I/O (S3, HTTP) inside transactions

## Locking Policy

| Scenario | Lock Type |
|----------|-----------|
| Counter updates, concurrent modifications | PESSIMISTIC_WRITE |
| Read-heavy, write-rare | OPTIMISTIC (`@Version` field) |

Pessimistic lock queries live in the Repository with an explicit timeout, and UseCases convert lock failures to a domain error:

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
@Query("SELECT f FROM Feed f WHERE f.id = :id")
fun findByIdWithLock(@Param("id") id: Long): Feed?
```

```kotlin
try {
    val feed = feedRepository.findByIdWithLock(feedId) ?: throw FeedNotFoundException()
} catch (e: PessimisticLockingFailureException) {
    throw ResourceLockedException()
}
```

Rules: always set lock timeouts, acquire locks in a consistent order, and surface lock failures as user-friendly domain errors.
