# API Design Rules

Use the `api-contract-update` skill when adding or changing controllers, endpoints, response/error codes, Swagger error examples, club/admin routes, DTO contracts, or pagination responses.

## Always-on Contract

- Every response is wrapped in `CommonResponse<T>` (`code`, `message`, `data`).
- Controllers return success with a domain `*ResponseCode`; errors use `CommonResponse.error(errorCode)`.
- Required controller annotations: `@Tag`, `@Operation(summary)`, `@ApiErrorCodeExample`, `@Valid` on request bodies, `@Parameter(hidden = true)` on internal params like `@CurrentUser`.
- Club resources use `/api/v4/clubs/{clubId}/...`; `clubId` is Base62 TSID and needs both `@TsidParam` and `@TsidPathVariable`.
- Admin club endpoints put `admin` before `clubs/{clubId}`: `/api/v4/admin/clubs/{clubId}/{resource}` for the single SecurityConfig rule `.requestMatchers("/api/v4/admin/**").hasRole("ADMIN")`.
- API codes use `XDDNN`; use the skill's code registry before adding success/error codes.
