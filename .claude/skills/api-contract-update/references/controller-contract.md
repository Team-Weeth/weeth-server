# Controller Contract Reference

## Controller Shape

```kotlin
@Tag(name = "USER", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class UserController(
    private val userUseCase: UserUseCase,
) {
    @GetMapping
    @Operation(summary = "내 정보 조회")
    fun getUser(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserResponse> =
        CommonResponse.success(USER_FIND_BY_ID_SUCCESS, userUseCase.find(userId))
}
```

Required on every controller:

- `@Tag`
- `@Operation(summary)` on each endpoint
- `@ApiErrorCodeExample`
- `@Valid` on request bodies
- `@Parameter(hidden = true)` on internal params such as `@CurrentUser`

## Club-scoped API

Club resources use `/api/v4/clubs/{clubId}/...`.

`clubId` is Base62 TSID, so use both annotations together:

```kotlin
@TsidParam
@TsidPathVariable clubId: Long
```

## Admin Endpoints

The `admin` prefix comes before `clubs/{clubId}`:

```text
/api/v4/admin/clubs/{clubId}/{resource}
```

This keeps the single SecurityConfig rule valid:

```kotlin
.requestMatchers("/api/v4/admin/**").hasRole("ADMIN")
```

## Response Format

Every response is wrapped in `CommonResponse<T>` with `code`, `message`, and `data`.

```kotlin
CommonResponse.success(USER_UPDATE_SUCCESS)
CommonResponse.success(USER_FIND_BY_ID_SUCCESS, data)
CommonResponse.error(errorCode)
```

Success enums implement `ResponseCodeInterface`, live in `domain/{domain}/presentation/{Domain}ResponseCode.kt`, and use Korean messages.

## REST Conventions

- Standard HTTP methods.
- Use `PATCH` for partial updates.
- Model actions as resource sub-paths, e.g. `POST /users/{userId}/activate`.
- Path variables identify resources.
- Query params filter and paginate, e.g. `?page=0&size=10&status=ACTIVE`.
