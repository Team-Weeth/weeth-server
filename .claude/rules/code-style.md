# Code Style Rules

## Language

- Primary: Kotlin (Java → Kotlin migration in progress)
- Build: Gradle (Kotlin DSL)

## Formatting

- Use ktlint
- Run `./gradlew ktlintFormat` before committing

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `UserController`, `UserSaveService` |
| Methods | camelCase | `getUserDetail`, `createUser` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Packages | lowercase | `com.example.domain.user` |
| DTOs | Suffix with purpose | `CreateUserRequest`, `UserResponse` |
| Test Fixtures | `{Entity}TestFixture` | `UserTestFixture` |

## Data Class vs Class

```kotlin
// Request DTO - Use data class
data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email val email: String
)

// Response DTO - Use data class
data class UserResponse(
    val id: Long,
    val name: String
)

// Entity - Use class (not data class)
@Entity
class User(
    @Id @GeneratedValue
    val id: Long = 0,
    var name: String
) : BaseEntity()
```

## Import Organization

1. Kotlin standard library
2. Third-party libraries
3. Spring framework
4. Project classes

## Constants

```kotlin
companion object {
    private const val MAX_PAGE_SIZE = 20
    private const val DEFAULT_PAGE_SIZE = 10
}
```

## Null Handling

```kotlin
// Use nullable types and Elvis operator
fun getUser(userId: Long): User =
    userRepository.findByIdOrNull(userId)
        ?: throw UserNotFoundException()
```