# Architecture Rules

## Package Structure

```text
src/main/kotlin/weeth/
├── domain/{domain-name}/
│   ├── application/
│   │   ├── dto/request/, dto/response/
│   │   ├── mapper/
│   │   ├── usecase/
│   │   │   ├── command/              # State-changing use cases
│   │   │   └── query/                # Read-only query services
│   │   ├── exception/
│   │   └── validator/
│   ├── domain/
│   │   ├── entity/                   # Rich Domain Model
│   │   ├── enums/
│   │   ├── port/                     # External system abstraction (Port interface)
│   │   ├── service/                  # Multi-entity business logic only
│   │   └── repository/
│   ├── infrastructure/               # Port implementations (Adapter)
│   └── presentation/
│       └── *Controller.kt
└── global/
    ├── auth/
    ├── config/
    └── common/
```

## Layer Dependencies

```text
presentation → application → domain (owns Port)
                                ↑
                           infrastructure (implements Port)
```

- **presentation** → application only
- **application** → domain (Repository, Entity, Service, Port). Never import infrastructure directly
- **domain** → depends on nothing. Owns Port interfaces
- **infrastructure** → implements domain/port. Depends on external libraries/SDK
- **Same domain**: UseCase uses Repository directly
- **Cross-domain read**: via target domain's Reader interface (not Repository directly)
- **Cross-domain write**: Repository directly (same transaction required)
- **Cross-domain write**: Use Domain Event (transaction separable)

## UseCase Rules

| Type | Package | Naming | Transaction |
|------|---------|--------|-------------|
| Command | `usecase/command/` | `{Verb}{Domain}UseCase` | `@Transactional` |
| Query | `usecase/query/` | `Get{Domain}QueryService` | `@Transactional(readOnly = true)` |

- **Orchestration only**: delegates business logic to Entity, calls Repository directly
- **No wrapper services**: do NOT create GetService/SaveService/DeleteService for thin Repository delegation
- **Group related actions**: e.g. `AuthUserUseCase` = login + signup + withdraw

## Query Service

- **Role**: data assembly for presentation (query, map, combine, paginate) — not business logic
- **Transaction**: `@Transactional(readOnly = true)`
- **Return type**: Response DTO
- **Prohibited**: state changes, business logic execution

### Command UseCase → Query Service dependency

| Situation | Recommendation |
|-----------|----------------|
| Simple `findById` + exception | Use Repository directly |
| Complex query returning Entity | Depend on Query Service OK |
| Query Service returns Response DTO | Do NOT depend — use Reader or Repository |

## Cross-domain Reference

- **Read**: Reader interface in target domain (`domain/repository/`), implemented by Repository
- **Write**: Repository directly (same transaction required)

## Entity (Rich Domain Model)

- **Factory method**: `companion object` with `create()` / `of()` including validation
- **State changes**: named methods (`publish()`, `softDelete()`) — no public setters
- **Validation**: `require` for argument checks, `check` for state preconditions
- **Business decisions**: `isEditableBy()`, `canPublish()` belong to Entity

## Domain Service

- **Only for multi-entity logic** or rules that don't fit a single Entity
- **No thin wrappers**: do NOT create `{Domain}GetService`, `{Domain}SaveService`
- **No `@Transactional`**: UseCase manages transaction boundaries
- **Name by role**: `AttendancePolicy`, `DuplicateCheckService`

## Port-Adapter Pattern

- **Port** (`domain/port/`): interface in domain language, no tech names → `FileStorage`, `PushNotificationSender`
- **Adapter** (`infrastructure/`): implementation with tech prefix → `S3FileStorage`, `FcmPushNotificationSender`
- UseCase depends on Port interface only → swappable, testable

## Core Principles

1. **Rich Domain Model**: Entity owns validation, state changes, and business decisions
2. **UseCase = orchestration**: coordinates flow; "how" is decided by Entity
3. **No meaningless services**: Repository wrappers are eliminated; Domain Service only for multi-entity logic
4. **Port-Adapter**: domain owns Port interfaces; infrastructure implements them
5. **Incremental migration**: migrate Java → Kotlin preserving existing structure
