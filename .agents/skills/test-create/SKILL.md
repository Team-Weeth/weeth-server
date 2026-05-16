---
name: test-create
description: Generate unit and integration tests for Kotlin Spring Boot applications using Kotest, MockK, springmockk, and Testcontainers. Use when the user asks to "write tests", "create test", "generate test", "add test coverage", or mentions testing specific classes/methods. Supports UseCase tests, controller tests, entity tests, and test fixtures.
---

# Test Generator

Generate focused Kotlin tests for the requested target.

## Workflow

### Step 1: Analyze Target Code

1. Read the source file to understand:
   - Class type (Controller, Service/UseCase, Repository, Entity)
   - Dependencies (injected fields)
   - Public methods to test
2. Read `.agents/rules/testing.md` before writing tests.

### Step 2: Determine Test Location

```
src/test/
└── kotlin/com/weeth/domain/{domain}/
    ├── application/usecase/
    │   ├── command/
    │   └── query/
    ├── domain/entity/
    ├── domain/service/
    ├── presentation/
    └── fixture/
```

Test file naming: `{ClassName}Test.kt`

### Step 3: Choose Test Style

| Class Type | Test Style | Framework |
|------------|------------|-----------|
| Command UseCase | DescribeSpec | Kotest + MockK |
| QueryService | DescribeSpec | Kotest + MockK |
| Entity / Domain Service | DescribeSpec or BehaviorSpec | Kotest |
| Validation / Simple Value Object | StringSpec | Kotest |
| Controller | @WebMvcTest + DescribeSpec | MockMvc + @MockkBean |

**Decision Guide:**
- **DescribeSpec**: Default choice for service tests. Clean describe/context/it structure.
- **BehaviorSpec**: Use for complex business logic requiring Given/When/Then BDD style.
- **StringSpec**: Use for simple validation or property tests.

### Step 4: Identify Test Cases

For each public method, create tests for:
- **Success case**: Valid input, expected output
- **Failure case**: Invalid input, expected exception
- **Edge cases**: Empty list, null values, boundary conditions
- **Soft delete**: Verify `deletedAt IS NULL` filtering if applicable

### Step 5: Generate Test Code

1. Create fixture if needed (in `fixture/` directory)
2. Write test class with proper annotations
3. Mock all dependencies
4. Implement test cases following given/when/then pattern
5. Add verification for mock interactions

See detailed examples:
- Kotlin: [references/kotlin-examples.md](references/kotlin-examples.md)

### Step 6: Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "CreateUserUseCaseTest"

# Run tests matching pattern
./gradlew test --tests "*UseCaseTest"

# Run with verbose output
./gradlew test --info
```

## Fixture Pattern

Create reusable test data builders in `fixture/` directory:

**Kotlin:**
```kotlin
object UserTestFixture {
    fun createUser(
        id: Long = 1L,
        email: String = "test@example.com",
        name: String = "Test User"
    ) = User(id = id, name = name, email = email)
}
```

## Controller Tests

Use @WebMvcTest for controller layer tests:
- Mock the UseCase/Service layer
- Test HTTP requests/responses
- Verify JSON serialization
- Check status codes and response structure

See [references/kotlin-examples.md](references/kotlin-examples.md) for complete examples.

## Checklist

Before completing:
- [ ] Success case test written
- [ ] Failure/exception case test written
- [ ] Edge case test written (empty, null, max value)
- [ ] Mock verification added (verify)
- [ ] Fixture created and reused
- [ ] Tests run successfully (`./gradlew test --tests "{TestClass}"`)

## Troubleshooting

### Test Compilation Errors

**Missing imports**: Check the existing Gradle test dependencies before adding anything. Prefer existing Kotest, MockK, springmockk, and Testcontainers versions already configured in the project.

### MockK "no answer found" errors

Use `relaxed = true` for dependencies you don't need to verify:
```kotlin
val repository = mockk<UserRepository>(relaxed = true)
```

### Soft Delete Tests Failing

Ensure repository method includes soft delete filtering:
```kotlin
// Correct
userRepository.findByIdAndDeletedAtIsNull(1L)

// Wrong
userRepository.findById(1L)  // Will include deleted entities
```

## References

- [Kotlin Examples (Kotest + MockK)](references/kotlin-examples.md)
