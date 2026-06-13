---
name: concurrency-safety
description: Implement or review Weeth transaction and concurrency behavior. Use when adding or changing @Transactional boundaries, pessimistic or optimistic locks, concurrent counter updates, same-transaction cross-domain writes, lock timeouts, or external I/O around transactions.
---

# Concurrency Safety

Use this skill for changes where transaction boundaries or concurrent writes can affect correctness.

## Workflow

### Step 1: Read Context

Read `.claude/rules/transaction-concurrency.md` and inspect the affected UseCase, Repository, Entity, and tests.

### Step 2: Classify The Risk

- Command UseCase: state change, requires `@Transactional`.
- Query Service: read-only assembly, requires `@Transactional(readOnly = true)`.
- Counter or concurrent modification: consider `PESSIMISTIC_WRITE`.
- Read-heavy and write-rare aggregate: consider optimistic locking with `@Version`.
- External I/O: keep outside the database transaction when possible.

### Step 3: Place Transaction Boundaries

- Put `@Transactional` on UseCase methods only.
- Do not put `@Transactional` on Domain Services.
- Keep transactions short.
- Cross-domain writes may use repositories directly when the same transaction is required.

### Step 4: Apply Locking Policy

Pessimistic lock queries live in the Repository with an explicit timeout.

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
@Query("SELECT f FROM Feed f WHERE f.id = :id")
fun findByIdWithLock(@Param("id") id: Long): Feed?
```

UseCases convert lock failures to domain errors.

```kotlin
try {
    val feed = feedRepository.findByIdWithLock(feedId) ?: throw FeedNotFoundException()
} catch (e: PessimisticLockingFailureException) {
    throw ResourceLockedException()
}
```

Always acquire multiple locks in a consistent order.

### Step 5: Verify

- Unit-test domain failure mapping where possible.
- Add integration tests for repository lock behavior or transaction behavior when correctness depends on the database.
- Run the narrowest relevant test first, then broaden if shared transaction behavior changed.

## Checklist

- [ ] UseCase owns the transaction boundary.
- [ ] No external S3/HTTP call is inside a transaction unless deliberately required.
- [ ] Pessimistic locks have explicit timeout hints.
- [ ] Lock failures become user-friendly domain errors.
- [ ] Multiple locks are acquired in a stable order.
