---
name: api-contract-update
description: Add or change Weeth API contracts. Use when creating or updating controllers, endpoints, CommonResponse usage, response/error codes, Swagger error examples, club/admin routes, request/response DTO contracts, or pagination responses. Do NOT use for pure domain logic with no API surface.
---

# API Contract Update

Keep API changes consistent with Weeth's controller, response-code, DTO, and Swagger conventions.

## Workflow

### Step 1: Load Required Context

Always read these rules first:

- `.claude/rules/api-design.md`
- `.claude/rules/mapper-dto.md`
- `.claude/rules/exception-handling.md`

Then read only the references needed for the task:

- Controller routes or annotations: `references/controller-contract.md`
- Success/error code additions: `references/code-registry.md`
- Exception or Swagger error examples: `references/error-docs.md`
- List or paginated responses: `references/pagination-response.md`

### Step 2: Inspect Local Patterns

Check the nearest existing controller, DTO, mapper, response-code enum, and error-code enum in the same domain before editing. Prefer the local domain's current naming and package layout when it does not violate the rules.

### Step 3: Apply the Contract

- Controllers return `CommonResponse<T>`.
- Success responses use the domain `*ResponseCode` enum.
- Request DTOs use Jakarta validation and `@field:Schema`.
- Response DTOs use `@Schema` and explicit nullable defaults for optional fields.
- Error codes use the `XDDNN` registry.
- Swagger error examples come from `@ApiErrorCodeExample` and `@ExplainError`, not hand-written response specs.

### Step 4: Verify

Run the narrowest relevant check:

```bash
./gradlew ktlintFormat
./gradlew test --tests "*ControllerTest"
./gradlew test --tests "*UseCaseTest"
```

Use broader `./gradlew test` or `./gradlew clean build` when the API change crosses domains or touches shared response/error infrastructure.

## Examples

### Example: Add Endpoint

User asks to add a new endpoint. Read `controller-contract.md`, inspect the domain controller and response code enum, add the controller method, DTO/mapper changes, and the success code.

### Example: Add Error Code

User asks to add validation or business failure handling. Read `code-registry.md` and `error-docs.md`, add the enum constant with `@ExplainError`, throw a `BaseException`, and declare the enum through `@ApiErrorCodeExample`.
