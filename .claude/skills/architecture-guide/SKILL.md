---
name: architecture-guide
description: "Show architecture patterns with code examples. Use when asked to 'show architecture', 'architecture example', 'how to structure', or when implementing new features/domains."
allowed-tools: Read, Glob, Grep
---

# Architecture Guide

Provide architecture pattern examples for the current task.
**All output MUST be written in Korean.**

## Reference: architecture rule

Always read `.claude/rules/architecture.md` first for core rules.

---

## UseCase Example

### Command UseCase

```kotlin
@Service
class CreatePostUseCase(
    private val postRepository: PostRepository,   // Same domain → Repository directly
    private val userReader: UserReader,            // Cross-domain → Reader interface
    private val fileStorage: FileStoragePort,          // Port interface
    private val postMapper: PostMapper             // Mapper
) {
    @Transactional
    fun execute(userId: Long, request: CreatePostRequest): PostResponse {
        val user = userReader.findById(userId)
            ?: throw UserNotFoundException()
        val imageUrl = fileStorage.upload(request.image)
        val post = Post.create(request.title, request.content, imageUrl, user)
        postRepository.save(post)
        return postMapper.toResponse(post)
    }
}
```

### Query Service

Query Service는 **조회 요청에 대한 데이터 조립** 계층이다. 비즈니스 로직이 아닌, 프레젠테이션을 위한 데이터 조합이 본질이다.

```kotlin
@Service
class GetPostQueryService(
    private val postRepository: PostRepository,
    private val postMapper: PostMapper
) {
    @Transactional(readOnly = true)
    fun findById(postId: Long): PostResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw PostNotFoundException()
        return postMapper.toResponse(post)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<PostResponse> {
        return postRepository.findAll(pageable)
            .map { postMapper.toResponse(it) }
    }
}
```

| 항목 | 규칙 |
|------|------|
| 역할 | 데이터 조회, 매핑, 조합, 페이징 |
| 트랜잭션 | `@Transactional(readOnly = true)` |
| 반환 타입 | Response DTO |
| 금지 사항 | 상태 변경, 비즈니스 로직 수행 |

### Command UseCase에서 Query Service 의존

| 상황 | 추천 |
|------|------|
| 단순 `findById` + 예외 | Repository 직접 호출 |
| Query Service가 Entity를 반환하는 복잡한 조회 | 의존 OK |
| Query Service가 Response DTO를 반환 | 의존하지 않음 |

### UseCase Does / Does Not

| Does (orchestration) | Does NOT (delegate to Entity) |
|----------------------|-------------------------------|
| Repository calls (find, save) | Business validation |
| Transaction boundary | State change logic |
| DTO ↔ Entity (Mapper) | Domain rule decisions |
| Port calls (external systems) | Value calculations, policy |

---

## Cross-domain Reference

### 읽기: Reader 인터페이스

타 도메인의 데이터를 조회할 때는 Repository 전체가 아닌 **읽기 전용 인터페이스**를 사용한다.

```kotlin
// user 도메인에 정의 (domain/repository/)
interface UserReader {
    fun findById(id: Long): User?
    fun existsById(id: Long): Boolean
}

// UserRepository가 상속
interface UserRepository : JpaRepository<User, Long>, UserReader
```

### 쓰기: Repository 직접 의존

타 도메인 쓰기가 필요한 경우 (같은 트랜잭션 필수):

```kotlin
@Service
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository  // Cross-domain write → Repository directly
) {
    @Transactional
    fun execute(request: CreateOrderRequest): OrderResponse {
        val product = productRepository.findByIdOrNull(request.productId)
            ?: throw ProductNotFoundException()
        product.decreaseStock(request.quantity)
        val order = Order.create(product, request.quantity)
        orderRepository.save(order)
        return orderMapper.toResponse(order)
    }
}
```

### Cross-domain 참조 요약

| 상황 | 방식 |
|------|------|
| 타 도메인 읽기 | Reader 인터페이스 |
| 타 도메인 쓰기 (같은 트랜잭션 필수) | Repository 직접 의존 |

---

## Entity (Rich Domain Model) Example

```kotlin
@Entity
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var title: String,
    var content: String,
    @Enumerated(EnumType.STRING)
    var status: PostStatus = PostStatus.DRAFT,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val author: User
) : BaseEntity() {

    companion object {
        fun create(title: String, content: String, imageUrl: String?, author: User): Post {
            require(title.isNotBlank()) { "Title must not be blank" }
            require(content.length <= 5000) { "Content must be 5000 chars or less" }
            return Post(title = title, content = content, author = author)
        }
    }

    fun publish() {
        check(status == PostStatus.DRAFT) { "Only DRAFT posts can be published" }
        status = PostStatus.PUBLISHED
    }

    fun update(title: String, content: String) {
        check(status != PostStatus.DELETED) { "Deleted posts cannot be updated" }
        this.title = title
        this.content = content
    }

    fun softDelete() {
        status = PostStatus.DELETED
    }

    fun isEditableBy(userId: Long): Boolean =
        author.id == userId
}
```

### Entity Patterns

| Pattern | How |
|---------|-----|
| Creation | `companion object` factory (`create`, `of`) with `require` validation |
| State change | Named methods (`publish`, `softDelete`) — no public setters |
| State validation | `check` for preconditions |
| Business decision | `isEditableBy()`, `canPublish()` |

---

## Domain Service Example

Only create when logic spans multiple entities:

```kotlin
@Service
class AttendancePolicy(
    private val attendanceRepository: AttendanceRepository
) {
    fun validateWeeklyLimit(user: User, date: LocalDate): Boolean {
        val weeklyCount = attendanceRepository.countByUserAndWeek(user.id, date)
        return weeklyCount < MAX_WEEKLY_ATTENDANCE
    }

    companion object {
        private const val MAX_WEEKLY_ATTENDANCE = 5
    }
}
```

### When to Create / Not Create

| Create (multi-entity logic) | Do NOT Create (use alternatives) |
|-----------------------------|----------------------------------|
| `TransferService` (cross-account) | `findById` + exception → UseCase calls Repository |
| `AttendancePolicy` (policy check) | `save` delegation → UseCase calls Repository |
| `DuplicateCheckService` (uniqueness) | Single entity state change → Entity method |

---

## Port-Adapter Example (FileStorage)

### Port (`domain/port/`)

```kotlin
interface FileStoragePort {
    fun upload(file: MultipartFile): String
    fun upload(files: List<MultipartFile>): List<String>
    fun delete(fileUrl: String)
}
```

### Adapter (`infrastructure/`)

```kotlin
@Component
class S3FileStorage(
    private val s3Client: S3Client,
    @Value("\${cloud.aws.s3.bucket}") private val bucket: String
) : FileStoragePort {

    override fun upload(file: MultipartFile): String {
        val key = generateKey(file.originalFilename)
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromInputStream(file.inputStream, file.size)
        )
        return "$CDN_URL/$key"
    }

    override fun upload(files: List<MultipartFile>): List<String> =
        files.map { upload(it) }

    override fun delete(fileUrl: String) {
        val key = extractKey(fileUrl)
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        )
    }
}
```

### Naming Convention

| Port (domain/port/)          | Adapter (infrastructure/) |
|------------------------------|---------------------------|
| `FileStoragePort`            | `S3FileStorage` |
| `PushNotificationSenderPort` | `FcmPushNotificationSender` |
| `CacheStorePort`             | `RedisCacheStore` |
