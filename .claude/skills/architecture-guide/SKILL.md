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

Query Service is the layer for **assembling data for read requests**. Its core purpose is presentation-oriented data composition, not business logic.

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

| Item | Rule |
|------|------|
| Role | Data retrieval, mapping, composition, paging |
| Transaction | `@Transactional(readOnly = true)` |
| Return type | Response DTO |
| Prohibited | State mutation, business logic execution |

### Query Service Dependency from Command UseCase

| Scenario | Recommendation |
|------|------|
| Simple `findById` + exception | Call Repository directly |
| Complex query where Query Service returns Entity | Dependency is acceptable |
| Query Service returns Response DTO | Do not depend on it |

### UseCase Does / Does Not

| Does (orchestration) | Does NOT (delegate to Entity) |
|----------------------|-------------------------------|
| Repository calls (find, save) | Business validation |
| Transaction boundary | State change logic |
| DTO ↔ Entity (Mapper) | Domain rule decisions |
| Port calls (external systems) | Value calculations, policy |

---

## Cross-domain Reference

### Read: Reader Interface

When reading data from another domain, use a **read-only interface** instead of the full Repository.

```kotlin
// Defined in user domain (domain/repository/)
interface UserReader {
    fun findById(id: Long): User?
    fun existsById(id: Long): Boolean
}

// UserRepository extends UserReader
interface UserRepository : JpaRepository<User, Long>, UserReader
```

### Write: Direct Repository Dependency

When cross-domain writes are required (same transaction is mandatory):

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

### Cross-domain Reference Summary

| Scenario | Approach |
|------|------|
| Cross-domain read | Reader interface |
| Cross-domain write (same transaction required) | Direct Repository dependency |

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
