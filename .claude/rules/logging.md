# Logging Rules

## Stack

- **SLF4J + Logback** (Spring Boot default)
- **AOP** for request/response logging
- **Loki** for log aggregation & querying

## Logger Creation

```kotlin
private val log = LoggerFactory.getLogger(javaClass)
```

## Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| `ERROR` | Exceptions, system failures | DB connection failure, unhandled exception |
| `WARN` | Expected but abnormal situations | Auth failure, rate limit, deprecated API call |
| `INFO` | Business events, state changes | User created, order completed, payment processed |
| `DEBUG` | Development diagnostics | Query parameters, intermediate calculations |

## AOP Request/Response Logging

```kotlin
@Aspect
@Component
class LoggingAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    fun logRequest(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request

        log.info("[REQ] {} {} | user={}",
            request?.method, request?.requestURI, extractUserId(request))

        val result = joinPoint.proceed()

        log.info("[RES] {} {} | status=OK",
            request?.method, request?.requestURI)

        return result
    }
}
```

## Logging Patterns

### UseCase/Service layer

```kotlin
@Transactional
fun execute(request: CreateUserRequest): UserResponse {
    log.info("Creating user: email={}", request.email)
    val user = User.create(request.name, request.email)
    userRepository.save(user)
    log.info("User created: id={}", user.id)
    return userMapper.toResponse(user)
}
```

### Exception logging

```kotlin
// GlobalExceptionHandler handles logging centrally
@ExceptionHandler(BaseException::class)
fun handleBaseException(e: BaseException): ResponseEntity<ErrorResponse> {
    log.warn("Business exception: code={}, message={}", e.errorCode.code, e.message)
    return ResponseEntity.status(e.errorCode.status).body(ErrorResponse.of(e.errorCode))
}

@ExceptionHandler(Exception::class)
fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected error", e)  // Include stack trace
    return ResponseEntity.internalServerError().body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR))
}
```

## Log Format (Loki-friendly)

Structured key-value pairs for easy Loki querying:

```
# Good - parseable by Loki
log.info("User created: id={}, email={}", user.id, user.email)
log.warn("Auth failed: ip={}, reason={}", clientIp, reason)

# Bad - hard to query
log.info("Created user " + user.id)
log.info("User $user created")  // toString() may expose sensitive data
```

## Security - Do NOT Log

| Category | Examples |
|----------|----------|
| Credentials | passwords, tokens, API keys, secrets |
| Personal info | SSN, phone number, address (log only if masked) |
| Full request body | May contain sensitive fields |
| Entity toString() | May include lazy-loaded relations or sensitive fields |

## Best Practices

1. **Use parameterized logging** (`log.info("msg: {}", val)`) - avoids string concatenation when log level is disabled
2. **Include stack trace for ERROR** (`log.error("msg", exception)`) - pass exception as last argument
3. **No stack trace for WARN** (`log.warn("msg: {}", e.message)`) - message only
4. **Log at boundaries** - API entry/exit (AOP), external service calls, async job start/end
5. **Don't log inside loops** - aggregate and log once after loop