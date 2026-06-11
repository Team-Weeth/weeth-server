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

Required on every controller: `@Tag`, `@Operation(summary)`, `@ApiErrorCodeExample`, `@Valid` on request bodies, `@Parameter(hidden = true)` on internal params like `@CurrentUser`.

## Club-scoped API

Club resources use `/api/v4/clubs/{clubId}/...`. `clubId` is Base62 TSID — use both annotations together:

```kotlin
@TsidParam        // Swagger (type: string)
@TsidPathVariable clubId: Long  // decodes Base62 → Long at runtime
```

### Admin Endpoints

`admin` prefix comes **before** `clubs/{clubId}`: `/api/v4/admin/clubs/{clubId}/{resource}` — enables a single SecurityConfig rule `.requestMatchers("/api/v4/admin/**").hasRole("ADMIN")`.

## Response Format

Every response is wrapped in `CommonResponse<T>` (`code`, `message`, `data`). Controllers return success with a domain ResponseCode enum directly: `CommonResponse.success(USER_UPDATE_SUCCESS)` or `CommonResponse.success(USER_FIND_BY_ID_SUCCESS, data)`. Errors use `CommonResponse.error(errorCode)`.

Success enums implement `ResponseCodeInterface`, live in `domain/{domain}/presentation/{Domain}ResponseCode.kt`. Messages are Korean.

## Code Format `XDDNN`

| Part | Meaning |
|------|---------|
| X | 1=Success, 2=Domain Error, 3=Infra/Server Error, 4=Client/Validation Error |
| DD | Domain ID (01~99) |
| NN | Sequence within domain (00~99) |

| DD | Domain | Success | Domain Error | Infra Error |
|----|------------|--------|--------|--------|
| 01 | account | 10100~ | 20100~ | — |
| 02 | attendance | 10200~ | 20200~ | — |
| 03 | session | 10300~ | 20300~ | — |
| 04 | board | 10400~ | 20400~ | — |
| 05 | comment | 10500~ | 20500~ | — |
| 06 | file | 10600~ | 20600~ | 30600~ |
| 07 | penalty | 10700~ | 20700~ | — |
| 08 | schedule | 10800~ | 20800~ | — |
| 09 | user | 10900~ | 20900~ | — |
| 10 | cardinal | 11000~ | 21000~ | — |
| 11 | club | 11100~ | 21100~ | — |
| 12 | dashboard | 11200~ | 21200~ | — |
| 13 | university | 11300~ | — | 31300~ |
| 90 | jwt/auth | — | 29000~ | — |
| 99 | common | — | — | 39900~ |

Enum naming/location is uniform: `{Domain}ResponseCode` in `presentation/`, `{Domain}ErrorCode` in `application/exception/`. Irregulars: schedule's error enum is `EventErrorCode`; JWT codes live in `global/auth/jwt/application/exception/JwtErrorCode`.

## REST Conventions

- Standard HTTP methods; PATCH for partial updates; actions on resources as sub-paths (`POST /users/{userId}/activate`)
- Path variables identify resources; query params filter/paginate (`?page=0&size=10&status=ACTIVE`)
