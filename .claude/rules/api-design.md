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

## Club-scoped API

Club resources use `/api/v4/clubs/{clubId}/...`. `clubId` is Base62 TSID — use three annotations together:

```kotlin
@PathVariable @TsidParam        // IDE warning suppression + Swagger (type: string)
@TsidPathVariable clubId: Long  // decodes Base62 → Long at runtime
```

## Required Annotations

| Annotation | Purpose |
|-----------|---------|
| `@Tag(name = "DOMAIN")` | OpenAPI grouping |
| `@Operation(summary = "...")` | API description |
| `@Parameter(hidden = true)` | Hide internal params from docs |
| `@Valid` | Enable validation |
| `@ApiErrorCodeExample(...)` | Auto-register error examples in Swagger |

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
    USER_FIND_ALL_SUCCESS(10900, HttpStatus.OK, "모든 회원 정보를 성공적으로 조회했습니다."),
    USER_FIND_BY_ID_SUCCESS(10907, HttpStatus.OK, "회원 정보가 성공적으로 조회되었습니다."),
    USER_UPDATE_SUCCESS(10908, HttpStatus.OK, "회원 정보가 성공적으로 수정되었습니다."),
}
```

- Success code enums must implement `ResponseCodeInterface`.
- Controllers should return success responses with enum directly:
  - `CommonResponse.success(USER_FIND_BY_ID_SUCCESS, data)`
  - `CommonResponse.success(USER_UPDATE_SUCCESS)`

## Code Format

|   | Mean            | Value                                                                      |
|---|-----------------|----------------------------------------------------------------------------|
| X | Category        | 1=Success, 2=Domain Error, 3=Infra/Server Error, 4=Client/Validation Error |
| DD | Domain ID       | 01~99                                                                      |
| NN | In Domain Count | 00~99                                                                      |

## Domain ID

| DD | Domain     | Success Range | Domain Error Range | Infra Error Range |
|----|------------|---------------|--------------------|-------------------|
| 01 | account    | 10100~        | 20100~             | —                 |
| 02 | attendance | 10200~        | 20200~             | —                 |
| 03 | session    | 10300~        | 20300~             | —                 |
| 04 | board      | 10400~        | 20400~             | —                 |
| 05 | comment    | 10500~        | 20500~             | —                 |
| 06 | file       | 10600~        | 20600~             | 30600~            |
| 07 | penalty    | 10700~        | 20700~             | —                 |
| 08 | schedule   | 10800~        | 20800~             | —                 |
| 09 | user       | 10900~        | 20900~             | —                 |
| 10 | cardinal   | 11000~        | 21000~             | —                 |
| 11 | club       | 11100~        | 21100~             | —                 |
| 12 | dashboard  | 11200~        | 21200~             | —                 |
| 13 | university | 11300~        | —                  | 31300~            |
| 90 | jwt/auth   | —             | 29000~             | —                 |
| 99 | common     | —             | —                  | 39900~            |

## Domain Success Codes

| Domain | ResponseCode Enum | Code Range | Location |
|--------|------------------|------------|----------|
| Account | `AccountResponseCode` | `101xx` | `domain/account/presentation/` |
| Attendance | `AttendanceResponseCode` | `102xx` | `domain/attendance/presentation/` |
| Session | `SessionResponseCode` | `103xx` | `domain/session/presentation/` |
| Board | `BoardResponseCode` | `104xx` | `domain/board/presentation/` |
| Comment | `CommentResponseCode` | `105xx` | `domain/comment/presentation/` |
| File | `FileResponseCode` | `106xx` | `domain/file/presentation/` |
| Penalty | `PenaltyResponseCode` | `107xx` | `domain/penalty/presentation/` |
| Schedule | `ScheduleResponseCode` | `108xx` | `domain/schedule/presentation/` |
| User | `UserResponseCode` | `109xx` | `domain/user/presentation/` |
| Cardinal | `CardinalResponseCode` | `110xx` | `domain/cardinal/presentation/` |
| Club | `ClubResponseCode` | `111xx` | `domain/club/presentation/` |
| Dashboard | `DashboardResponseCode` | `112xx` | `domain/dashboard/presentation/` |
| University | `UniversityResponseCode` | `113xx` | `domain/university/presentation/` |

## Domain Error Codes

| Domain | ErrorCode Enum | Code Range | Location |
|--------|---------------|------------|----------|
| Account | `AccountErrorCode` | `201xx` | `domain/account/application/exception/` |
| Attendance | `AttendanceErrorCode` | `202xx` | `domain/attendance/application/exception/` |
| Session | `SessionErrorCode` | `203xx` | `domain/session/application/exception/` |
| Board | `BoardErrorCode` | `204xx` | `domain/board/application/exception/` |
| Comment | `CommentErrorCode` | `205xx` | `domain/comment/application/exception/` |
| File | `FileErrorCode` | `206xx` (domain), `306xx` (infra) | `domain/file/application/exception/` |
| Penalty | `PenaltyErrorCode` | `207xx` | `domain/penalty/application/exception/` |
| Schedule | `EventErrorCode` | `208xx` | `domain/schedule/application/exception/` |
| User | `UserErrorCode` | `209xx` | `domain/user/application/exception/` |
| Cardinal | `CardinalErrorCode` | `210xx` | `domain/cardinal/application/exception/` |
| Club | `ClubErrorCode` | `211xx` | `domain/club/application/exception/` |
| Dashboard | `DashboardErrorCode` | `212xx` | `domain/dashboard/application/exception/` |
| University | `UniversityErrorCode` | `313xx` (infra) | `domain/university/application/exception/` |
| JWT (Global) | `JwtErrorCode` | `290xx` | `global/auth/jwt/application/exception/` |

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

### Admin Endpoints

`admin` prefix comes **before** `clubs/{clubId}`: `/api/v4/admin/clubs/{clubId}/{resource}`

```
/api/v4/clubs/{clubId}/boards          # user-facing
/api/v4/admin/clubs/{clubId}/boards    # admin
```

Enables a single SecurityConfig rule: `.requestMatchers("/api/v4/admin/**").hasRole("ADMIN")`

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
