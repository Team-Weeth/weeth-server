# Architecture Rules

## Package Structure

```text
src/main/kotlin/com/weeth/
├── domain/{domain-name}/
│   ├── application/
│   │   ├── dto/request/, dto/response/
│   │   ├── mapper/
│   │   ├── usecase/command/          # State-changing use cases
│   │   ├── usecase/query/            # Read-only query services
│   │   ├── exception/
│   │   └── validator/
│   ├── domain/
│   │   ├── entity/                   # Rich Domain Model
│   │   ├── vo/                       # Value Objects
│   │   ├── enums/
│   │   ├── port/                     # External system abstraction
│   │   ├── service/                  # Multi-entity business logic only
│   │   └── repository/               # JpaRepository + Reader interfaces
│   ├── infrastructure/               # Port implementations (Adapter)
│   └── presentation/                 # Controller + ResponseCode
└── global/                           # auth, config, common, logging
```

## Layer Dependencies

```text
presentation → application → domain (owns Port)
                                ↑
                           infrastructure (implements Port)
```

- application never imports infrastructure; domain depends on nothing
- **Same domain**: UseCase uses Repository directly
- **Cross-domain read**: via target domain's Reader interface (in `domain/repository/`), never its Repository
- **Cross-domain write**: Repository directly when the same transaction is required; otherwise use a Domain Event

## UseCase Rules

| Type | Package | Naming | Transaction |
|------|---------|--------|-------------|
| Command | `usecase/command/` | `{Verb}{Domain}UseCase` | `@Transactional` |
| Query | `usecase/query/` | `Get{Domain}QueryService` | `@Transactional(readOnly = true)` |

- **Orchestration only**: business logic lives in Entities; UseCase coordinates flow
- **No wrapper services**: never create GetService/SaveService/DeleteService for thin Repository delegation
- **Group related actions**: e.g. `AuthUserUseCase` = login + signup + withdraw
- Query Service does data assembly for presentation (query, map, combine, paginate) and returns Response DTOs — no state changes, no business logic

### Command UseCase → Query Service dependency

| Situation | Recommendation |
|-----------|----------------|
| Simple `findById` + exception | Use Repository directly |
| Complex query returning Entity | Depend on Query Service OK |
| Query Service returns Response DTO | Do NOT depend — use Reader or Repository |

## Entity (Rich Domain Model)

- State changes via named methods (`publish()`, `softDelete()`) — no public setters; mutable props use `private set`
- `require` for argument checks, `check` for state preconditions; business decisions (`isEditableBy()`) belong to the Entity

### Constructor Pattern

Primary constructor takes **business creation params only** (non-property). JPA-managed fields (`id`, `isDeleted`) go in the body with `private set` and defaults. Validation lives in a `companion object` `create()` factory (or named mutation methods), not the constructor. Simple entities with trivial creation (e.g. `Board`) may use a public constructor without a factory. Use `architecture-guide` for full examples.

## Value Object (`domain/vo/`)

- **Single field**: Kotlin `@JvmInline value class` with `require` in `init`; stored as primitive in the Entity (VO conversion at UseCase/Service boundary)
- **Multi field**: `@Embeddable class` (**NOT** `data class`) used via `@Embedded` — same `private set` pattern as Entity, `require` in `init`, `companion object` `of`/`from` factory when normalization is needed. Default identity `equals` is fine (compared as part of the owning Entity); override only when value-equality is required.
- VOs may contain operations/decisions relevant to the value (e.g. `Period.contains(date)`)

## Domain Service

- Only for multi-entity logic or rules that don't fit a single Entity — name by role (`AttendancePolicy`, `DuplicateCheckService`)
- No `@Transactional` (UseCase manages boundaries), no thin wrappers

## Port-Adapter

- Port (`domain/port/`): interface in domain language — `FileStoragePort`, `PushNotificationSenderPort`
- Adapter (`infrastructure/`): implementation with tech prefix — `S3FileStorageAdapter`, `FcmPushNotificationSenderAdapter`
- UseCase depends on the Port interface only
