# Testing Rules

## Stack & Styles

Kotest + MockK + springmockk (`@MockkBean`) + Testcontainers (MySQL). No Mockito.

| Kotest Style | Use Case |
|-------|----------|
| `DescribeSpec` | Default for application tests (Command UseCase, QueryService) |
| `BehaviorSpec` | Complex business logic requiring BDD (Given/When/Then) |
| `StringSpec` | Simple validation and pure domain logic tests |

Test packages mirror source. Test class: `{ClassName}Test`. Shared fixtures: `src/test/kotlin/com/weeth/domain/{domain}/fixture/{Entity}TestFixture` — `object` with factory methods and sensible defaults for all parameters.

## Architecture-aligned Unit Boundaries

- Command UseCase test: mock Repository/Reader/Port, verify orchestration behavior
- QueryService test: verify read-only assembly (query/map/combine/paginate), no state mutation
- Entity test: verify `create/of`, state transitions, `require`/`check`, business decisions
- Domain Service test: only for multi-entity logic/policy classes
- Controller test: request/response contract with `@WebMvcTest`

Mocking rules: same-domain → mock Repository directly; cross-domain read → mock Reader (not target Repository); application tests mock Port interfaces, never infrastructure adapters.

Integration tests (`@SpringBootTest`/`@WebMvcTest` + Testcontainers) are for DB queries, API endpoints, and transaction behavior; everything else is a plain unit test with MockK.

## What to Test / Skip

**Test:** UseCase orchestration paths (success/failure/branching), mock interaction contracts (`verify`), QueryService assembly/pagination, entity invariants and state transitions, exception scenarios and error-code mapping.

**Skip:** thin delegation without logic, getters/trivial DTO mapping, framework functionality.

## Mock Lifecycle in DescribeSpec (pitfall)

MockK mocks are **not** cleared between `it` blocks — accumulated invocations break `verify(exactly = N)`. When mocks are shared, always:

```kotlin
beforeTest {
    clearMocks(repository)
    every { repository.save(any()) } answers { firstArg() }  // re-stub defaults
}
```
