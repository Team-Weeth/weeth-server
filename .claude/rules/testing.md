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
| `DescribeSpec` | Default for application tests (Command UseCase, QueryService) |
| `BehaviorSpec` | Complex business logic requiring BDD (Given/When/Then) |
| `StringSpec` | Simple validation and pure domain logic tests |

## Directory Structure

```text
src/test/kotlin/weeth/domain/{domain-name}/
├── application/usecase/command/   # Command UseCase tests
├── application/usecase/query/     # QueryService tests
├── domain/service/                # Domain service tests (multi-entity logic)
├── domain/entity/                 # Entity behavior tests
├── presentation/                  # Controller tests (@WebMvcTest)
└── fixture/                       # Shared fixtures for the domain
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Test class | `{ClassName}Test` | `CreateUserUseCaseTest`, `GetUserQueryServiceTest` |
| Test fixture | `{Entity}TestFixture` | `UserTestFixture` |
| DescribeSpec description | method/action + condition + behavior | `describe("execute") { context("with valid request") { it("creates user") } }` |

## Architecture-aligned Unit Boundaries

- Command UseCase test: mock Repository/Reader/Port, verify orchestration behavior.
- QueryService test: verify read-only assembly (query/map/combine/paginate), no state mutation.
- Entity test: verify `create/of`, state transitions, `require/check`, and business decisions.
- Domain Service test: only for multi-entity logic/policy classes (not thin wrappers).
- Controller test: verify request/response contract and serialization with `@WebMvcTest`.

## Dependency Rules in Tests

- Same-domain dependencies: UseCase mocks Repository directly.
- Cross-domain read: mock target domain Reader interface (not target Repository directly).
- Cross-domain write: mock target domain Repository directly when same-transaction write is required.
- Port-Adapter: application tests mock Port interface, not infrastructure adapter implementations.

## Unit Test vs Integration Test

| Category | Unit Test | Integration Test |
|----------|-----------|-----------------|
| Scope | Single class | Multiple layers / external systems |
| Dependencies | MockK mocks | Testcontainers (DB, Redis) |
| Speed | Fast (ms) | Slow (seconds) |
| Annotation | None | `@SpringBootTest`, `@WebMvcTest` |
| When to use | Orchestration, branching, entity/domain rules | DB queries, API endpoints, transaction behavior |

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

- Location: `src/test/kotlin/weeth/domain/{domain-name}/fixture/`
- Use `object` with factory methods
- Provide sensible defaults for all parameters
- Reuse across test classes in the same domain

## What to Test / Skip

**Write tests for:**
- UseCase orchestration paths (success/failure/branching)
- Reader/Repository/Port interaction contracts (`verify`)
- QueryService data assembly and pagination mapping
- Entity invariants and state transitions (`require`/`check`)
- Exception scenarios and error-code mapping

**Skip tests for:**
- Thin wrapper methods that only delegate to Repository without logic
- Getter/setter, trivial DTO mapping
- Framework-provided functionality

## Mock Lifecycle in DescribeSpec

MockK mocks are **not** automatically cleared between `it` blocks. Without clearing, accumulated invocations cause `verify(exactly = N)` to fail in subsequent tests.

Always add `beforeTest { clearMocks(...) }` when mocks are shared:

```kotlin
class SomeUseCaseTest : DescribeSpec({
    val repository = mockk<SomeRepository>()
    val useCase = SomeUseCase(repository)

    beforeTest {
        clearMocks(repository)
        // Re-stub defaults after clearing
        every { repository.save(any()) } answers { firstArg() }
    }

    describe("someMethod") {
        it("case 1") { verify(exactly = 1) { repository.save(any()) } }
        it("case 2") { verify(exactly = 1) { repository.save(any()) } } // OK - count reset
    }
})
```

## Running Tests

```bash
./gradlew test                              # All tests
./gradlew test --tests "*UseCaseTest"       # Pattern match
./gradlew test --tests "CreateUserUseCaseTest" # Specific class
```
