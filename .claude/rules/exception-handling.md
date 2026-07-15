# Exception Handling Rules

- Domain exceptions extend `BaseException(errorCode)`.
- Error codes are per-domain enums implementing `ErrorCodeInterface` (`code`, `status`, `message`); messages are Korean.
- Error code numbering follows `XDDNN`; use `api-contract-update` before adding or changing error codes.
- Swagger error examples are generated from `@ApiErrorCodeExample` and `@ExplainError`; never hand-edit response specs.
- `ExceptionDocController` is only for aggregated exception browsing in Swagger; no business logic there.
- There is no common error enum yet. `CommonExceptionHandler` uses `CommonResponse.createFailure()` directly; if one is introduced, use DD=99.
