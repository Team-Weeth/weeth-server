# API Design Rules

## Controller Structure

```kotlin
@Tag(name = "USER", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class UserController(
    private val userUsecase: UserUsecase
) {
    @GetMapping
    @Operation(summary = "내 정보 조회")
    fun getUser(@Parameter(hidden = true) @CurrentUser userId: Long): CommonResponse<UserResponse> =
        CommonResponse.success(USER_FIND_BY_ID_SUCCESS, userUsecase.find(userId))
}
```

## Required Annotations

| Annotation | Purpose |
|-----------|---------|
| `@Tag(name = "DOMAIN")` | OpenAPI grouping |
| `@Operation(summary = "...")` | API description |
| `@Parameter(hidden = true)` | Hide internal params from docs |
| `@Valid` | Enable validation |

## Response Format

Wrap responses in `CommonResponse`:

```kotlin
data class CommonResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        @JvmStatic
        fun success(responseCode: ResponseCodeInterface): CommonResponse<Void?> =
            CommonResponse(code = responseCode.code, message = responseCode.message, data = null)

        @JvmStatic
        fun <T> success(responseCode: ResponseCodeInterface, data: T): CommonResponse<T> =
            CommonResponse(code = responseCode.code, message = responseCode.message, data = data)

        @JvmStatic
        fun error(errorCode: ErrorCodeInterface): CommonResponse<Void?> =
            CommonResponse(code = errorCode.code, message = errorCode.message, data = null)
    }
}
```

## Response Codes

```kotlin
enum class UserResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ResponseCodeInterface {
    GET_MY_INFO(1100, HttpStatus.OK, "내 정보 조회에 성공했습니다."),
    GET_USER_INFO(1101, HttpStatus.OK, "다른 사용자 정보 조회에 성공했습니다."),
    UPDATE_PROFILE_IMAGE(1102, HttpStatus.OK, "프로필 이미지 수정에 성공했습니다.")
}
```

## Code Numbering

| Range | Category |
|-------|----------|
| 1XXX | Success responses |
| 2XXX | Domain-specific errors |
| 3XXX | Server errors |
| 4XXX | Client errors |

## HTTP Methods

| Method | Usage |
|--------|-------|
| GET | Read operations, no body |
| POST | Create resources |
| PUT | Full updates |
| PATCH | Partial updates |
| DELETE | Remove resources |

## Path Design

```
GET    /users                    # List users
GET    /users/{userId}           # Get single user
POST   /users                    # Create user
PATCH  /users/{userId}           # Update user
DELETE /users/{userId}           # Delete user
POST   /users/{userId}/activate  # Action on resource
```

## Query & Path Parameters

- Query params for filtering: `?page=0&size=10&status=ACTIVE`
- Path variables for resource identification: `/users/{userId}`

## Request/Response DTO

```kotlin
// Request
data class CreateUserRequest(
    @field:Schema(description = "User name", example = "John Doe")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Email address", example = "john@example.com")
    @field:NotBlank
    @field:Email
    val email: String
)

// Response
data class UserResponse(
    @Schema(description = "User ID", example = "1")
    val id: Long,

    @Schema(description = "User name", example = "John Doe")
    val name: String,

    @Schema(description = "Email address", example = "john@example.com")
    val email: String?
)
```

## Validation

Use Jakarta validation annotations in DTOs:
- `@NotNull`, `@NotEmpty`, `@NotBlank`
- `@Size(min = 1, max = 100)`
- `@Positive`, `@PositiveOrZero`
- `@Email`, `@Pattern`
