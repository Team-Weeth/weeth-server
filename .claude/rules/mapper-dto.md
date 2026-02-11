# Mapper & DTO Rules

## Mapper Pattern

AS-IS (Java): MapStruct 사용 → TO-BE (Kotlin): 수동 Mapper 패턴으로 마이그레이션

```kotlin
@Component
class UserMapper(
    private val profileMapper: ProfileMapper
) {
    fun toResponse(user: User) = UserResponse(
        id = user.id,
        name = user.name,
        email = user.email,
        profile = profileMapper.toResponse(user.profile)
    )

    fun toEntity(request: CreateUserRequest) = User(
        name = request.name.trim(),
        email = request.email.lowercase(),
        status = UserStatus.ACTIVE
    )
}
```

## Mapper Naming

| Method Pattern | Purpose |
|---------------|---------|
| `toResponse` | Entity → Response DTO |
| `toEntity` | Request DTO → Entity |
| `toDto` | Entity → Generic DTO |
| `from{Source}` | Convert from specific source type |

## Request DTO

Located in `application/dto/request/`:

```kotlin
data class CreateUserRequest(
    @field:Schema(description = "User name", example = "John Doe")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Email address", example = "john@example.com")
    @field:NotBlank
    @field:Email
    val email: String,

    @field:Schema(description = "Profile settings")
    @field:Valid
    @field:NotNull
    val profile: ProfileRequest
)
```

### Validation Annotations

| Annotation | Usage |
|-----------|-------|
| `@NotNull` | Field must not be null |
| `@NotEmpty` | Collection must have elements |
| `@NotBlank` | String must not be empty/whitespace |
| `@Size(min, max)` | Length/size constraints |
| `@Positive` | Number must be > 0 |
| `@Valid` | Validate nested objects |

## Response DTO

Located in `application/dto/response/`:

```kotlin
data class UserResponse(
    @Schema(description = "User ID", example = "1")
    val id: Long,

    @Schema(description = "User name", example = "John Doe")
    val name: String,

    @Schema(description = "Email address", example = "john@example.com")
    val email: String,

    @Schema(description = "Profile information")
    val profile: ProfileResponse? = null,

    @Schema(description = "Active status", example = "true")
    val isActive: Boolean? = null
)
```

### Response DTO Rules

- Use `@Schema` for OpenAPI documentation
- Use non-nullable types for required fields
- Use nullable types with default `null` for optional fields

## List Response with Pagination

```kotlin
data class UserListResponse(
    @Schema(description = "User list")
    val users: List<UserResponse>,

    @Schema(description = "Pagination info")
    val page: PageResponse
)

data class PageResponse(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
) {
    companion object {
        fun from(page: Page<*>) = PageResponse(
            pageNumber = page.number,
            pageSize = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext()
        )
    }
}
```

## Mapper Dependencies

Mappers can inject other mappers:

```kotlin
@Component
class UserMapper(
    private val profileMapper: ProfileMapper,
    private val addressMapper: AddressMapper
)
```

## Mapper Testing

```kotlin
class UserMapperTest : StringSpec({
    val profileMapper = mockk<ProfileMapper>()
    val userMapper = UserMapper(profileMapper)

    "toResponse should map all fields" {
        val user = UserTestFixture.createUser(1L, "test@example.com")
        every { profileMapper.toResponse(any()) } returns ProfileResponse(id = 1L)

        val response = userMapper.toResponse(user)

        response.id shouldBe 1L
        response.email shouldBe "test@example.com"
    }
})
```
