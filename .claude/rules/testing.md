# Testing Rules

## Frameworks

| Framework | Purpose |
|-----------|---------|
| Kotest | Kotlin test framework |
| MockK | Kotlin mocking |
| springmockk | Spring bean mocking (`@MockkBean`) |
| Testcontainers | Integration tests (DB, Redis, etc.) |

## Test Styles (Kotest)

| Style | Use Case |
|-------|----------|
| `DescribeSpec` | Default for service/usecase tests (describe/context/it) |
| `BehaviorSpec` | Complex business logic requiring BDD (Given/When/Then) |
| `StringSpec` | Simple validation, utility tests |

## Directory Structure
todo: 아키텍처 변경시 동기화 필요
```
src/test/kotlin/com/example/app/domain/{domain}/
├── application/usecase/       # UseCase unit tests
├── domain/service/            # Domain service unit tests
├── presentation/              # Controller tests (@WebMvcTest)
└── fixture/                   # Test fixtures (shared test data)
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Test class | `{ClassName}Test` | `UserGetServiceTest` |
| Test fixture | `{Entity}TestFixture` | `UserTestFixture` |
| Test method (DescribeSpec) | describe: method name, context: condition, it: expected behavior | `describe("findById") { context("when user exists") { it("should return user") } }` |

## Unit Test vs Integration Test

| Category | Unit Test | Integration Test |
|----------|-----------|-----------------|
| Scope | Single class | Multiple layers / external systems |
| Dependencies | MockK mocks | Testcontainers (DB, Redis) |
| Speed | Fast (ms) | Slow (seconds) |
| Annotation | None | `@SpringBootTest`, `@WebMvcTest` |
| When to use | Business logic, branching, calculations | DB queries, API endpoints, transaction behavior |

## Fixture Pattern

```kotlin
object UserTestFixture {
    fun createUser(
        id: Long = 1L,
        email: String = "test@example.com",
        name: String = "Test User"
    ) = User(id = id, name = name, email = email, status = UserStatus.ACTIVE)
}
```

- Location: `src/test/kotlin/{domain}/fixture/`
- Use `object` with factory methods
- Provide sensible defaults for all parameters
- Reuse across test classes in the same domain

## What to Test / Skip

**Write tests for:**
- Business logic with conditions/branching
- Exception scenarios
- Complex transformations or calculations
- Transaction boundaries

**Skip tests for:**
- Simple CRUD delegation (findById, save, delete)
- Getter/setter, trivial DTO mapping
- Framework-provided functionality

## Running Tests

```bash
./gradlew test                              # All tests
./gradlew test --tests "*ServiceTest"       # Pattern match
./gradlew test --tests "UserGetServiceTest" # Specific class
```