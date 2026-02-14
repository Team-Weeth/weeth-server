---
name: test-create
description: Generate unit and integration tests for Java/Kotlin Spring Boot applications using JUnit 5/Kotest + Mockito/MockK. Use when the user asks to "write tests", "create test", "generate test", "add test coverage", or mentions testing specific classes/methods. Supports service tests, controller tests, and test fixtures.
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Test Generator

Generate comprehensive tests for: $ARGUMENTS

## Workflow

### Step 1: Analyze Target Code

1. Read the source file to understand:
   - Language (Java or Kotlin)
   - Class type (Controller, Service/UseCase, Repository, Entity)
   - Dependencies (injected fields)
   - Public methods to test

### Step 2: Determine Test Location

```
src/test/
├── java/com/example/app/domain/{domain}/
│   ├── application/usecase/     # UseCase unit tests
│   ├── domain/service/          # Service unit tests
│   ├── presentation/            # Controller tests
│   └── fixture/                 # Test fixtures
└── kotlin/com/example/app/domain/{domain}/
    ├── application/usecase/
    ├── domain/service/
    ├── presentation/
    └── fixture/
```

Test file naming: `{ClassName}Test.{java|kt}`

### Step 3: Choose Test Style

| Language | Class Type | Test Style | Framework |
|----------|------------|------------|-----------|
| Java | Service/UseCase | JUnit 5 + Mockito | @ExtendWith(MockitoExtension.class) |
| Java | Controller | @WebMvcTest | MockMvc + @MockBean |
| Kotlin | Service/UseCase (recommended) | DescribeSpec | Kotest + MockK |
| Kotlin | Service/UseCase (BDD) | BehaviorSpec | Kotest + MockK |
| Kotlin | Validation/Simple | StringSpec | Kotest |
| Kotlin | Controller | @WebMvcTest + DescribeSpec | MockMvc + @MockkBean |

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
- Java: [references/java-examples.md](references/java-examples.md)
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

**Java:**
```java
public class UserTestFixture {
    public static User createUser(Long id, String email) {
        return User.builder()
            .id(id)
            .name("Test User")
            .email(email)
            .status(UserStatus.ACTIVE)
            .build();
    }
}
```

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

See [references/java-examples.md](references/java-examples.md) and [references/kotlin-examples.md](references/kotlin-examples.md) for complete examples.

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

**Missing imports**: Add these dependencies to build.gradle:
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'io.kotest:kotest-runner-junit5:5.x.x'  // Kotlin only
testImplementation 'io.mockk:mockk:1.x.x'                  // Kotlin only
```

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

- [Java Examples (JUnit 5 + Mockito)](references/java-examples.md)
- [Kotlin Examples (Kotest + MockK)](references/kotlin-examples.md)
