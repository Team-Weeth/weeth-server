# Error And Swagger Documentation Reference

## Exception Structure

Domain exceptions extend `BaseException(errorCode)`.

```kotlin
class UserNotFoundException : BaseException(UserErrorCode.USER_NOT_FOUND)
```

Error-code enums implement `ErrorCodeInterface`.

```kotlin
enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(20900, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
}
```

Messages are Korean. Code numbering follows `XDDNN` in `code-registry.md`.

There is no common error enum yet. `CommonExceptionHandler` uses `CommonResponse.createFailure()` directly. If a common error enum is introduced, use DD=99.

## Swagger Auto-documentation

Error examples are auto-registered in Swagger from annotations. Do not hand-edit response specs.

- `@ApiErrorCodeExample(SomeErrorCode::class, ...)` on a controller class for shared errors.
- `@ApiErrorCodeExample(...)` on a method for endpoint-specific errors; method-level wins over class-level.
- `@ExplainError("...")` on each enum constant; it falls back to `message` when missing.
- `ExceptionDocController` is only for aggregated exception browsing in Swagger; do not add business logic there.

## Adding A New Exception

1. Add an enum constant to the proper `*ErrorCode` with `@ExplainError`.
2. Create or adjust the exception class extending `BaseException`.
3. Ensure the relevant controller or method declares the enum in `@ApiErrorCodeExample`.
4. Verify Swagger shows the new code when practical.
