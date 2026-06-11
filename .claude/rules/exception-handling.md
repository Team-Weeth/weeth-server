# Exception Handling Rules

## Structure

Domain exceptions extend `BaseException(errorCode)`; error codes are per-domain enums implementing `ErrorCodeInterface` (`code: Int`, `status: HttpStatus`, `message: String`).

```kotlin
class UserNotFoundException : BaseException(UserErrorCode.USER_NOT_FOUND)

enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(20900, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
}
```

Error messages are Korean. Code numbering follows `XDDNN` (see api-design.md). There is no common error enum yet — `CommonExceptionHandler` uses `CommonResponse.createFailure()` directly; if one is introduced, use DD=99.

## Swagger Auto-documentation

Error examples are auto-registered in Swagger from annotations — never hand-edit response specs:

- `@ApiErrorCodeExample(SomeErrorCode::class, ...)` on a controller class (shared errors) or method (endpoint-specific; method wins over class)
- `@ExplainError("...")` on each enum constant (falls back to `message` if missing)
- `ExceptionDocController` exists only for aggregated exception browsing in Swagger — no business logic there

## When Adding a New Exception

1. Add enum constant to the proper `*ErrorCode` with `@ExplainError`
2. Create/adjust the exception class extending `BaseException`
3. Ensure the relevant controller/method declares the enum in `@ApiErrorCodeExample`
4. Verify Swagger shows the new code
