# Code Style Rules

## Language & Formatting

- Kotlin only — do not introduce Java production code. Build: Gradle (Kotlin DSL)
- ktlint enforced: run `./gradlew ktlintFormat` before committing

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| DTOs | Suffix with purpose | `CreateUserRequest`, `UserResponse` |
| Test Fixtures | `{Entity}TestFixture` | `UserTestFixture` |
| Constants | SCREAMING_SNAKE_CASE in `companion object` | `MAX_PAGE_SIZE` |

## Null Safety

- Avoid `!!`. Prefer `?.`, `?:`, `requireNotNull`/`checkNotNull`.
- If `!!` is truly unavoidable, add a short comment explaining why.

## Class Kinds

- Entity: regular `class` (not `data class`), mutable props use `private set` + named mutation methods
- DTO: `data class`

## Comments

- Do NOT comment self-explanatory code. Comment only for:
  - Core business logic — explain "why", not "what"
  - Non-obvious implementation: performance workarounds, external system constraints
  - Architecture decisions: reason for choosing a specific pattern
- KDoc (`/** */`) for public APIs, Port interfaces, and external contracts; inline `//` for intent within methods
