# Exception Handling Rules

## Exception Hierarchy

```
RuntimeException
    └── BaseException (abstract)
            ├── UserNotFoundException
            ├── OrderNotFoundException
            └── ... (domain-specific exceptions)
```

## Base Exception

```kotlin
abstract class BaseException(
    val errorCode: ErrorCodeInterface,
    message: String? = null
) : RuntimeException(message ?: errorCode.message)
```

## Error Code Interface

```kotlin
interface ErrorCodeInterface {
    val code: Int
    val status: HttpStatus
    val message: String

    fun getExplainError(): String = message
}
```

## Domain Error Codes

```kotlin
enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(20900, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),

    @ExplainError("가입 승인 대기 중인 사용자가 접근을 시도할 때 발생합니다.")
    USER_INACTIVE(20901, HttpStatus.FORBIDDEN, "가입 승인이 허가되지 않은 계정입니다."),

    @ExplainError("이미 가입된 이메일로 회원가입을 시도할 때 발생합니다.")
    USER_EXISTS(20902, HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다."),
}
```

## Common Error Codes

```kotlin
enum class CommonErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    // 3DDNN: Infra/Server errors (DD=99 for common)
    INTERNAL_SERVER_ERROR(39901, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    JSON_PROCESSING_ERROR(39902, HttpStatus.INTERNAL_SERVER_ERROR, "JSON processing error"),

    // 4DDNN: Client/Validation errors (DD=99 for common)
    INVALID_ARGUMENT(49901, HttpStatus.BAD_REQUEST, "Invalid argument"),
    RESOURCE_NOT_FOUND(49903, HttpStatus.NOT_FOUND, "Resource not found"),
}
```

## Domain Exception Classes

```kotlin
class UserNotFoundException : BaseException(UserErrorCode.USER_NOT_FOUND)
```

## Swagger Exception Documentation (Auto)

Swagger is customized so exception codes/examples are registered automatically from annotations and error-code enums.

### Required Annotations

- `@ApiErrorCodeExample`: Declare which `ErrorCodeInterface` enums can be returned by an API.
- `@ExplainError`: Optional field-level description for richer Swagger examples.

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeExample(
    vararg val value: KClass<out ErrorCodeInterface>
)
```

### Controller Convention

- Apply `@ApiErrorCodeExample` at controller class level when most endpoints share the same domain errors.
- Apply it at method level when a specific endpoint has different error sets.
- If both are present, method-level declaration should take precedence for that endpoint.
- If multiple enums are needed, pass them together:

```kotlin
@ApiErrorCodeExample(BoardErrorCode::class, NoticeErrorCode::class)
class NoticeController
```

### ErrorCode Enum Convention

- Domain error enums must implement `ErrorCodeInterface`.
- Add `@ExplainError` to each enum constant when possible.
- If `@ExplainError` is missing, fallback to `message`.

```kotlin
enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")
    USER_NOT_FOUND(20900, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
}
```

### Documentation-only Controller

- Keep an `ExceptionDocController` for aggregated, domain-wide exception browsing in Swagger.
- This controller is for documentation only; it should not contain business logic.

### When Adding a New Exception

1. Add enum constant to the proper `*ErrorCode`.
2. Add `@ExplainError` description.
3. Create/adjust domain exception class extending `BaseException`.
4. Ensure the relevant controller/method has `@ApiErrorCodeExample` for that enum.
5. Verify Swagger examples show the new code without manual response-spec edits.
