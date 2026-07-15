# Mapper & DTO Rules

## Mapper Pattern

Manual `@Component` Mapper classes — **no MapStruct**. Mappers may inject other mappers.

```kotlin
@Component
class UserMapper {
    fun toResponse(user: User) = UserResponse(id = user.id, name = user.name)
    fun toEntity(request: CreateUserRequest) = User(name = request.name.trim(), ...)
}
```

| Method | Purpose |
|--------|---------|
| `toResponse` | Entity → Response DTO |
| `toEntity` | Request DTO → Entity |
| `toDto` | Entity → generic DTO |
| `from{Source}` | Convert from specific source type |

## DTO Rules

- Location: `application/dto/request/`, `application/dto/response/`
- Request DTO: Jakarta validation + `@field:Schema(description, example)` on every field

```kotlin
data class CreateUserRequest(
    @field:Schema(description = "User name", example = "John Doe")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)
```

- Response DTO: `@Schema` on every field; non-nullable for required fields, nullable + default `null` for optional
- Use the `api-contract-update` skill for paginated/list response templates.

### PATCH (partial update) request DTOs

A `PATCH` endpoint = **partial update**, never full replacement. The `Update{X}Request` shape and the Entity mutation method must both honor this — a non-null field on a PATCH DTO forces clients to resend unchanged data and silently wipes fields the request omits.

- **Every field nullable with default `null`**; `@Schema(description = "... (null=변경 안 함)", nullable = true)`. Do NOT put `@NotBlank`/`@NotNull` on PATCH fields (they reject the "unchanged" null) — keep only bound checks like `@Size`/`@Positive`, which pass on null.
- The Entity mutation method takes nullable params and applies each only when non-null (`field?.let { ... }`), so omitted fields keep their existing value. See `Post.update` / `AccountTransaction.update` for the canonical shape.
- Never pass a hardcoded `null` from the UseCase for a field the request doesn't carry — that resets it on every edit. Omit it (rely on the param default) so the existing value is preserved.
- Full-replacement semantics? Use `PUT`, not `PATCH`.

### Query-param enums (pagination / sort / filter)

Sort/filter/page enums are a request-side API contract → `application/dto/request/`, never `domain/enums/` (which is for persisted/business values). A sort enum owns its Spring `Sort` mapping via a `toSort()` member.
