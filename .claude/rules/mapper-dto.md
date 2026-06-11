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
