# Swagger 성공 응답 자동 문서화 — 개선안 (Kotlin)

## 1. 개선 요약

| 항목 | 기존 문제 | 개선 |
|------|-----------|------|
| 제네릭 타입 소실 | `CommonResponse<List<SomeDto>>`의 내부 타입을 추론 못함 | `ResolvableType` 전체를 전파하여 중첩 제네릭까지 재귀 탐색 |
| 어노테이션 타입 안전성 | `names = {"LOGIN_SUCCESS"}` 문자열 배열 → 오타·리팩터링 누락 | `@ApiSuccessCodeExample(ResponseCode::class)` 단일 어노테이션 + enum 클래스 기반 리졸버 |
| 언어 | Java 기반 | Kotlin 전면 전환 (data class, reified, sealed 등 활용) |

---

## 2. 전제 조건

- Spring Boot 3.x + Kotlin
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`
- 공통 응답 포맷:

```kotlin
data class CommonResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
```

- 성공 코드 인터페이스:

```kotlin
interface ResponseCodeInterface {
    val code: Int
    val status: HttpStatus
    val message: String
}
```

---

## 3. 어노테이션 설계 — 단일 전역 어노테이션

### 3.1 핵심 아이디어

문자열로 enum 이름을 넘기지 않고, enum 클래스 자체를 어노테이션 파라미터로 넘긴다.

### 3.2 전략: 단일 어노테이션 + enum 클래스 기반 리졸버

`ApiErrorCodeExample`와 같은 패턴으로, 성공 코드도 단일 어노테이션에서 enum 클래스를 받는다.

```kotlin
package com.example.global.swagger.annotation

import com.example.global.response.ResponseCodeInterface
import kotlin.reflect.KClass

/**
 * 성공 코드 enum 클래스를 직접 받는 단일 어노테이션.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiSuccessCodeExample(
    vararg val value: KClass<out ResponseCodeInterface>
)
```

**사용 예시:**

```kotlin
@PostMapping("/login")
@ApiSuccessCodeExample(AuthResponseCode::class)
fun login(@RequestBody request: LoginRequest): CommonResponse<LoginResponse> { ... }
```

문자열 상수 없이 enum 클래스 자체를 참조하므로, 클래스명 오타/리팩터링 누락은 컴파일 타임에 잡힌다.

### 3.3 통합 리졸버

단일 어노테이션에서 `ResponseCodeInterface` 목록을 추출하는 유틸:

```kotlin
package com.example.global.swagger.resolver

import com.example.global.response.ResponseCodeInterface
import org.springframework.web.method.HandlerMethod

/**
 * HandlerMethod에서 성공 코드 어노테이션을 찾아
 * ResponseCodeInterface 목록으로 통합 반환한다.
 */
object SuccessCodeResolver {

    fun resolve(handler: HandlerMethod): List<ResponseCodeInterface> {
        val annotation = handler.getMethodAnnotation(ApiSuccessCodeExample::class.java)
            ?: handler.beanType.getAnnotation(ApiSuccessCodeExample::class.java)
            ?: return emptyList()

        return annotation.value
            .asSequence()
            .map { it.java }
            .filter { it.isEnum }
            .flatMap { enumClass ->
                (enumClass.enumConstants ?: emptyArray())
                    .asSequence()
                    .filterIsInstance<ResponseCodeInterface>()
            }
            .distinctBy { it.code }
            .toList()
    }
}
```

> **확장 시**: 새 도메인 `ResponseCode` enum 생성 후 컨트롤러에 `@ApiSuccessCodeExample(NewResponseCode::class)`만 선언하면 된다.

---

## 4. 제네릭 타입 소실 해결

### 4.1 핵심 변경: `Class<*>` 대신 `ResolvableType` 전파

기존 코드의 문제:

```
resolvePayloadType → Class<?> 반환
→ List<UserDto>가 List.class로 소실
→ createSample(List.class) → List.of("sample")
```

개선:

```
resolvePayloadType → ResolvableType 반환
→ List<UserDto>의 제네릭 정보 유지
→ createSample(ResolvableType) → [{ userId: 1, name: "sample" }]
```

### 4.2 페이로드 타입 추론

```kotlin
private fun resolvePayloadType(method: Method): ResolvableType {
    var returnType = ResolvableType.forMethodReturnType(method)

    // ResponseEntity<T> → T
    if (ResponseEntity::class.java.isAssignableFrom(returnType.resolve(Any::class.java))) {
        returnType = returnType.getGeneric(0)
    }

    // CommonResponse<T> → T
    if (CommonResponse::class.java.isAssignableFrom(returnType.resolve(Any::class.java))) {
        returnType = returnType.getGeneric(0)
    }

    return returnType
}
```

### 4.3 샘플 생성 — `ResolvableType` 기반 재귀

```kotlin
private fun createSample(type: ResolvableType, depth: Int, visiting: MutableSet<Class<*>>): Any? {
    if (depth > MAX_SAMPLE_DEPTH) return null

    val resolved = type.resolve() ?: return null

    // void / Unit
    if (resolved == Void::class.java || resolved == Void.TYPE || resolved == Unit::class.java) return null

    // 프리미티브 / 잘 알려진 타입
    primitiveSample(resolved)?.let { return it }

    // Enum
    if (resolved.isEnum) {
        return resolved.enumConstants?.firstOrNull()?.toString()
    }

    // List / Set → 내부 제네릭 타입으로 재귀
    if (List::class.java.isAssignableFrom(resolved) || Set::class.java.isAssignableFrom(resolved)) {
        val elementType = type.getGeneric(0)
        val elementResolved = elementType.resolve()
        return if (elementResolved != null && elementResolved != Any::class.java) {
            listOf(createSample(elementType, depth + 1, visiting) ?: "sample")
        } else {
            listOf("sample")
        }
    }

    // Map → key/value 제네릭으로 재귀
    if (Map::class.java.isAssignableFrom(resolved)) {
        val keyType = type.getGeneric(0)
        val valueType = type.getGeneric(1)
        val keyResolved = keyType.resolve() ?: String::class.java
        val valueSample = createSample(valueType, depth + 1, visiting) ?: "value"
        val keySample = primitiveSample(keyResolved) ?: "key"
        return mapOf(keySample to valueSample)
    }

    // Array
    if (resolved.isArray) {
        val componentType = ResolvableType.forArrayComponent(type)
        return listOf(createSample(componentType, depth + 1, visiting) ?: "sample")
    }

    // 순환 참조 방지
    if (resolved in visiting) return null
    visiting.add(resolved)

    return try {
        createObjectSample(resolved, depth, visiting)
    } finally {
        visiting.remove(resolved)
    }
}
```

### 4.4 프리미티브 / 공통 타입 커버리지 확장

```kotlin
private fun primitiveSample(type: Class<*>): Any? = when (type) {
    // 문자열
    String::class.java, CharSequence::class.java -> "sample"
    java.lang.String::class.java -> "sample"

    // 숫자
    Long::class.java, java.lang.Long::class.java, Long::class.javaPrimitiveType -> 1L
    Int::class.java, java.lang.Integer::class.java, Int::class.javaPrimitiveType -> 1
    Short::class.java, java.lang.Short::class.java, Short::class.javaPrimitiveType -> 1.toShort()
    Double::class.java, java.lang.Double::class.java, Double::class.javaPrimitiveType -> 1.0
    Float::class.java, java.lang.Float::class.java, Float::class.javaPrimitiveType -> 1.0f
    Boolean::class.java, java.lang.Boolean::class.java, Boolean::class.javaPrimitiveType -> true

    // 날짜/시간
    java.time.LocalDate::class.java -> "2026-01-01"
    java.time.LocalDateTime::class.java -> "2026-01-01T00:00:00"
    java.time.LocalTime::class.java -> "00:00:00"
    java.time.Instant::class.java -> "2026-01-01T00:00:00Z"
    java.time.ZonedDateTime::class.java -> "2026-01-01T00:00:00+09:00"
    java.time.OffsetDateTime::class.java -> "2026-01-01T00:00:00+09:00"

    // 기타 자주 쓰는 타입
    java.util.UUID::class.java -> "550e8400-e29b-41d4-a716-446655440000"
    java.math.BigDecimal::class.java -> 0
    java.math.BigInteger::class.java -> 0
    java.net.URI::class.java -> "https://example.com"
    java.net.URL::class.java -> "https://example.com"

    else -> null
}
```

### 4.5 객체 샘플 생성 — Kotlin data class + 상속 지원

```kotlin
/**
 * Kotlin data class와 일반 클래스 모두 처리.
 * 상속 구조의 필드도 포함하기 위해 상위 클래스까지 순회한다.
 */
private fun createObjectSample(type: Class<*>, depth: Int, visiting: MutableSet<Class<*>>): Map<String, Any?> {
    val result = LinkedHashMap<String, Any?>()

    // Kotlin data class → constructor 파라미터 기반 (선언 순서 보장)
    val kotlinClass = type.kotlin
    val primaryConstructor = kotlinClass.primaryConstructor

    if (primaryConstructor != null) {
        for (param in primaryConstructor.parameters) {
            val name = param.name ?: continue
            val field = findField(type, name) ?: continue
            val schema = field.getAnnotation(Schema::class.java)
            val fieldType = resolveSchemaType(schema, field.type)
            val value = schemaExampleValue(schema, fieldType)
                ?: createSample(ResolvableType.forField(field), depth + 1, visiting)
            result[name] = value
        }
    } else {
        // 일반 클래스 — 상속 필드 포함
        for (field in getAllFields(type)) {
            if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue
            val schema = field.getAnnotation(Schema::class.java)
            val fieldType = resolveSchemaType(schema, field.type)
            val value = schemaExampleValue(schema, fieldType)
                ?: createSample(ResolvableType.forField(field), depth + 1, visiting)
            result[field.name] = value
        }
    }

    return result
}

/** 상속 계층 전체의 필드를 순회 (Object/Any 제외) */
private fun getAllFields(type: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    var current: Class<*>? = type
    while (current != null && current != Any::class.java && current != Object::class.java) {
        fields.addAll(current.declaredFields)
        current = current.superclass
    }
    return fields
}

/** data class 필드 탐색: 선언된 클래스 + 상위 클래스 */
private fun findField(type: Class<*>, name: String): Field? {
    var current: Class<*>? = type
    while (current != null && current != Any::class.java) {
        try {
            return current.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    return null
}
```

---

## 5. 전체 Customizer 통합 코드

```kotlin
package com.example.global.swagger.config

import com.example.global.response.CommonResponse
import com.example.global.response.ResponseCodeInterface
import com.example.global.swagger.resolver.SuccessCodeResolver
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ResolvableType
import org.springframework.http.ResponseEntity
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.full.primaryConstructor

@Configuration
class SwaggerSuccessConfig {

    companion object {
        private const val MAX_SAMPLE_DEPTH = 3
    }

    @Bean
    fun successOperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        val responseCodes = SuccessCodeResolver.resolve(handlerMethod)
        if (responseCodes.isEmpty()) return@OperationCustomizer operation

        val payloadType = resolvePayloadType(handlerMethod.method)

        responseCodes.forEach { code ->
            addSuccessExample(operation.responses, code, payloadType)
        }

        operation
    }

    // ── 페이로드 타입 추론 ──────────────────────────────────

    private fun resolvePayloadType(method: Method): ResolvableType {
        var returnType = ResolvableType.forMethodReturnType(method)

        if (ResponseEntity::class.java.isAssignableFrom(returnType.resolve(Any::class.java))) {
            returnType = returnType.getGeneric(0)
        }
        if (CommonResponse::class.java.isAssignableFrom(returnType.resolve(Any::class.java))) {
            returnType = returnType.getGeneric(0)
        }

        return returnType
    }

    // ── Example 주입 ───────────────────────────────────────

    private fun addSuccessExample(responses: ApiResponses, responseCode: ResponseCodeInterface, payloadType: ResolvableType) {
        val status = responseCode.status.value()
        val apiResponse = responses.computeIfAbsent(status.toString()) { ApiResponse() }

        if (apiResponse.description.isNullOrBlank()) {
            apiResponse.description = responseCode.message
        }

        // 204, 205는 body 없음
        if (status == 204 || status == 205) return

        val mediaType = getOrCreateMediaType(apiResponse)
        val enumName = (responseCode as Enum<*>).name
        val exampleName = "[$enumName] ${responseCode.message}"

        // 이미 존재하면 skip (수동 정의 우선)
        if (mediaType.examples?.containsKey(exampleName) == true) return

        val example = Example().apply {
            description = responseCode.message
            value = createSuccessEnvelope(responseCode, payloadType)
        }
        mediaType.addExamples(exampleName, example)
    }

    private fun createSuccessEnvelope(responseCode: ResponseCodeInterface, payloadType: ResolvableType): Map<String, Any?> {
        return linkedMapOf(
            "code" to responseCode.code,
            "message" to responseCode.message,
            "data" to createSample(payloadType, 0, mutableSetOf())
        )
    }

    // ── 샘플 생성 (ResolvableType 기반) ────────────────────

    private fun createSample(type: ResolvableType, depth: Int, visiting: MutableSet<Class<*>>): Any? {
        if (depth > MAX_SAMPLE_DEPTH) return null
        val resolved = type.resolve() ?: return null

        if (resolved == Void::class.java || resolved == Void.TYPE || resolved == Unit::class.java) return null

        primitiveSample(resolved)?.let { return it }

        if (resolved.isEnum) return resolved.enumConstants?.firstOrNull()?.toString()

        // List / Set
        if (List::class.java.isAssignableFrom(resolved) || Set::class.java.isAssignableFrom(resolved)) {
            val elementType = type.getGeneric(0)
            val elementResolved = elementType.resolve()
            return if (elementResolved != null && elementResolved != Any::class.java) {
                listOf(createSample(elementType, depth + 1, visiting) ?: "sample")
            } else {
                listOf("sample")
            }
        }

        // Map
        if (Map::class.java.isAssignableFrom(resolved)) {
            val valueType = type.getGeneric(1)
            val valueSample = createSample(valueType, depth + 1, visiting) ?: "value"
            return mapOf("key" to valueSample)
        }

        // Array
        if (resolved.isArray) {
            val componentResolvable = type.componentType
            return listOf(createSample(componentResolvable, depth + 1, visiting) ?: "sample")
        }

        if (resolved in visiting) return null
        visiting.add(resolved)

        return try {
            createObjectSample(resolved, depth, visiting)
        } finally {
            visiting.remove(resolved)
        }
    }

    private fun createObjectSample(type: Class<*>, depth: Int, visiting: MutableSet<Class<*>>): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        val primaryConstructor = type.kotlin.primaryConstructor

        if (primaryConstructor != null) {
            for (param in primaryConstructor.parameters) {
                val name = param.name ?: continue
                val field = findField(type, name) ?: continue
                val schema = field.getAnnotation(Schema::class.java)
                val fieldType = resolveSchemaType(schema, field.type)
                val value = schemaExampleValue(schema, fieldType)
                    ?: createSample(ResolvableType.forField(field), depth + 1, visiting)
                result[name] = value
            }
        } else {
            for (field in getAllFields(type)) {
                if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue
                val schema = field.getAnnotation(Schema::class.java)
                val fieldType = resolveSchemaType(schema, field.type)
                val value = schemaExampleValue(schema, fieldType)
                    ?: createSample(ResolvableType.forField(field), depth + 1, visiting)
                result[field.name] = value
            }
        }

        return result
    }

    // ── 유틸 ───────────────────────────────────────────────

    private fun primitiveSample(type: Class<*>): Any? = when (type) {
        String::class.java, java.lang.String::class.java, CharSequence::class.java -> "sample"

        Long::class.java, java.lang.Long::class.java, Long::class.javaPrimitiveType -> 1L
        Int::class.java, java.lang.Integer::class.java, Int::class.javaPrimitiveType -> 1
        Short::class.java, java.lang.Short::class.java, Short::class.javaPrimitiveType -> 1.toShort()
        Double::class.java, java.lang.Double::class.java, Double::class.javaPrimitiveType -> 1.0
        Float::class.java, java.lang.Float::class.java, Float::class.javaPrimitiveType -> 1.0f
        Boolean::class.java, java.lang.Boolean::class.java, Boolean::class.javaPrimitiveType -> true

        java.time.LocalDate::class.java -> "2026-01-01"
        java.time.LocalDateTime::class.java -> "2026-01-01T00:00:00"
        java.time.LocalTime::class.java -> "00:00:00"
        java.time.Instant::class.java -> "2026-01-01T00:00:00Z"
        java.time.ZonedDateTime::class.java -> "2026-01-01T00:00:00+09:00"
        java.time.OffsetDateTime::class.java -> "2026-01-01T00:00:00+09:00"

        java.util.UUID::class.java -> "550e8400-e29b-41d4-a716-446655440000"
        java.math.BigDecimal::class.java -> 0
        java.math.BigInteger::class.java -> 0
        java.net.URI::class.java -> "https://example.com"
        java.net.URL::class.java -> "https://example.com"

        else -> null
    }

    private fun resolveSchemaType(schema: Schema?, fallback: Class<*>): Class<*> {
        if (schema == null) return fallback
        val impl = schema.implementation.java
        if (impl == Void::class.java || impl == Void.TYPE) return fallback
        return impl
    }

    private fun schemaExampleValue(schema: Schema?, type: Class<*>): Any? {
        if (schema == null || schema.example.isNullOrBlank()) return null
        return parseByType(schema.example, type)
    }

    private fun parseByType(example: String, type: Class<*>): Any = try {
        when (type) {
            Long::class.java, java.lang.Long::class.java, Long::class.javaPrimitiveType -> example.toLong()
            Int::class.java, java.lang.Integer::class.java, Int::class.javaPrimitiveType -> example.toInt()
            Short::class.java, java.lang.Short::class.java, Short::class.javaPrimitiveType -> example.toShort()
            Double::class.java, java.lang.Double::class.java, Double::class.javaPrimitiveType -> example.toDouble()
            Float::class.java, java.lang.Float::class.java, Float::class.javaPrimitiveType -> example.toFloat()
            Boolean::class.java, java.lang.Boolean::class.java, Boolean::class.javaPrimitiveType -> example.toBoolean()
            else -> example
        }
    } catch (_: RuntimeException) {
        example
    }

    private fun getAllFields(type: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java && current != Object::class.java) {
            fields.addAll(current.declaredFields)
            current = current.superclass
        }
        return fields
    }

    private fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            try { return current.getDeclaredField(name) }
            catch (_: NoSuchFieldException) { current = current.superclass }
        }
        return null
    }

    private fun getOrCreateMediaType(apiResponse: ApiResponse): MediaType {
        val content = apiResponse.content ?: Content().also { apiResponse.content = it }
        return content["application/json"] ?: MediaType().also { content.addMediaType("application/json", it) }
    }
}
```

---

## 6. 컨트롤러 적용 예시

```kotlin
@RestController
@RequestMapping("/feeds")
@ApiSuccessCodeExample(FeedResponseCode::class)
class FeedController(
    private val feedUsecase: FeedUsecase,
) {
    @GetMapping
    fun getFeeds(): CommonResponse<FeedListResponse> {
        return CommonResponse.success(FeedResponseCode.GET_ALL_FEED, feedUsecase.getFeeds(...))
    }
}
```

클래스 레벨(`@Target.CLASS`)에 선언하면 도메인 컨트롤러 전체에 전역 적용할 수 있고, 특정 API만 다른 enum 세트를 쓰고 싶으면 메서드 레벨에서 override 할 수 있다.

---

## 7. DTO 예시

```kotlin
data class LoginResponse(
    @field:Schema(example = "1")
    val userId: Long,

    @field:Schema(example = "홍길동")
    val name: String,

    @field:Schema(example = "5")
    val cardinal: Int,

    @field:Schema(example = "BE")
    val position: String,

    @field:Schema(example = "eyJ...access")
    val accessToken: String,

    @field:Schema(example = "eyJ...refresh")
    val refreshToken: String
)

data class MemberResponse(
    @field:Schema(example = "42")
    val id: Long,

    @field:Schema(example = "김철수")
    val name: String,

    @field:Schema(example = "BACKEND")
    val part: String
)
```

> **주의**: Kotlin data class에서는 `@Schema`가 아니라 `@field:Schema`로 써야 필드에 어노테이션이 붙는다. `@Schema`만 쓰면 생성자 파라미터에 붙어서 리플렉션으로 필드 조회 시 누락된다.

---

## 8. 기대 결과: 제네릭 타입 해결 전후 비교

### `CommonResponse<List<MemberResponse>>` 응답

**개선 전:**

```json
{
  "code": 2000,
  "message": "멤버 조회 성공",
  "data": ["sample"]
}
```

**개선 후:**

```json
{
  "code": 2000,
  "message": "멤버 조회 성공",
  "data": [
    {
      "id": 42,
      "name": "김철수",
      "part": "BACKEND"
    }
  ]
}
```

### `CommonResponse<Map<String, MemberResponse>>` 응답

```json
{
  "code": 2000,
  "message": "멤버 맵 조회 성공",
  "data": {
    "key": {
      "id": 42,
      "name": "김철수",
      "part": "BACKEND"
    }
  }
}
```

---

## 9. 새 도메인 추가 시 체크리스트

1. `XxxResponseCode` enum 생성 — `ResponseCodeInterface` 구현 확인
2. 컨트롤러(클래스 또는 메서드)에 `@ApiSuccessCodeExample(XxxResponseCode::class)` 적용
3. DTO에 `@field:Schema(example = ...)` 보강
4. `/v3/api-docs`에서 example 확인

---

## 10. 트러블슈팅

### 10.1 `@field:Schema`를 빠뜨림

**증상**: DTO 필드가 전부 `"sample"`, `1` 등 기본값으로 나옴

**원인**: Kotlin data class에서 `@Schema`만 쓰면 constructor parameter에 붙음. `field.getAnnotation(Schema::class.java)`로 조회 시 `null` 반환.

**해결**: `@field:Schema(example = "...")`로 변경

### 10.2 `List<T>`가 여전히 `["sample"]`로 나옴

**원인**: 컨트롤러 반환 타입이 raw type이거나 `*`로 선언됨

**해결**: 반환 타입을 `CommonResponse<List<MemberResponse>>`처럼 구체적으로 명시

### 10.3 에러 문서화 어노테이션과 충돌

**원인**: 같은 status code에 성공/에러 양쪽에서 example을 넣을 때 등록 순서에 따라 동작이 다름

**해결**: `@Order`로 Customizer 순서 명시, 또는 example name prefix로 구분 (`[SUCCESS]`, `[ERROR]`)

### 10.4 상속 DTO 필드 누락

기존 코드의 `getDeclaredFields()`는 현재 클래스만 조회. 개선안의 `getAllFields()`가 상위 클래스까지 순회하므로 해결됨.

### 10.5 `Can't compute ClassId for primitive type: long/boolean` 경고

**증상**: Swagger 생성 중 특정 API success example이 skip됨

**원인**: 일부 필드에서 `ResolvableType.forField(field)`가 primitive 타입 분석 중 예외를 던짐

**해결**: `forField` 실패 시 `ResolvableType.forClass(field.type)`로 fallback하도록 방어
