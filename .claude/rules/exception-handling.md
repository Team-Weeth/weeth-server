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
}
```

## Domain Error Codes

```kotlin
enum class UserErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    USER_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(2002, HttpStatus.CONFLICT, "User already exists"),
    INVALID_CREDENTIALS(2003, HttpStatus.UNAUTHORIZED, "Invalid credentials")
}
```

## Common Error Codes

```kotlin
enum class CommonErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String
) : ErrorCodeInterface {
    // 3XXX: Server errors
    INTERNAL_SERVER_ERROR(3001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    JSON_PROCESSING_ERROR(3002, HttpStatus.INTERNAL_SERVER_ERROR, "JSON processing error"),
    RESOURCE_LOCKED(3003, HttpStatus.CONFLICT, "Resource is locked"),

    // 4XXX: Client errors
    INVALID_ARGUMENT(4001, HttpStatus.BAD_REQUEST, "Invalid argument"),
    RESOURCE_NOT_FOUND(4003, HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(4004, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    UNAUTHORIZED(4005, HttpStatus.UNAUTHORIZED, "Authentication required")
}
```

## Domain Exception Classes

```kotlin
class UserNotFoundException : BaseException(UserErrorCode.USER_NOT_FOUND)
```

## Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ResponseEntity<ErrorResponse> {
        log.error("BaseException: {}", e.message)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse.of(e.errorCode))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(CommonErrorCode.INVALID_ARGUMENT, errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: ", e)
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }
}
```

## Error Response

```kotlin
data class ErrorResponse(
    val code: Int,
    val message: String,
    val errors: List<FieldErrorResponse>? = null
) {
    companion object {
        fun of(errorCode: ErrorCodeInterface) = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message
        )

        fun of(errorCode: ErrorCodeInterface, fieldErrors: List<FieldError>) = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            errors = fieldErrors.map { FieldErrorResponse.of(it) }
        )
    }
}

data class FieldErrorResponse(
    val field: String,
    val message: String?,
    val rejectedValue: Any?
) {
    companion object {
        fun of(error: FieldError) = FieldErrorResponse(
            field = error.field,
            message = error.defaultMessage,
            rejectedValue = error.rejectedValue
        )
    }
}
```