# Transaction & Concurrency Rules

Use the `concurrency-safety` skill when changing transaction boundaries, lock behavior, concurrent counter updates, same-transaction cross-domain writes, or external I/O around transactions.

## Transaction Placement

- `@Transactional` goes on **UseCase** methods only — Command: `@Transactional`, Query: `@Transactional(readOnly = true)`
- Domain Services must NOT have `@Transactional`; UseCase owns transaction boundaries
- Keep transactions short — no external I/O (S3, HTTP) inside transactions

## Locking Policy

| Scenario | Lock Type |
|----------|-----------|
| Counter updates, concurrent modifications | PESSIMISTIC_WRITE |
| Read-heavy, write-rare | OPTIMISTIC (`@Version` field) |

Rules: pessimistic lock queries live in the Repository, always set lock timeouts, acquire locks in a consistent order, and surface lock failures as user-friendly domain errors.
