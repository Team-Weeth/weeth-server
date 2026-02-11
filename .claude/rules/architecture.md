# Architecture Rules

## Package Structure

### AS-IS (Java)

```text
src/main/java/leets/weeth/
├── domain/
│   ├── account/
│   ├── attendance/
│   ├── board/
│   ├── comment/
│   ├── file/
│   ├── penalty/
│   ├── schedule/
│   └── user/
└── global/
    ├── auth/
    ├── config/
    ├── common/
    └── sas/
```

```text
domain/{domain-name}/
├── application/
│   ├── dto/
│   ├── mapper/
│   ├── usecase/
│   ├── exception/
│   └── validator/
├── domain/
│   ├── entity/
│   ├── enums/
│   ├── service/
│   └── repository/
└── presentation/
    └── *Controller.java
```

### TO-BE (Kotlin)

```text
src/main/kotlin/weeth/
├── domain/
│   ├── account/
│   ├── attendance/
│   ├── board/
│   ├── comment/
│   ├── file/
│   ├── penalty/
│   ├── schedule/
│   └── user/
└── global/
    ├── auth/
    ├── config/
    └── common/
```

```text
domain/{domain-name}/
├── application/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/
│   ├── usecase/
│   │   ├── command/                        # 상태 변경 유스케이스
│   │   │   ├── AuthUserUseCase.kt          # 로그인, 회원가입, 탈퇴
│   │   │   └── UpdateUserUseCase.kt        # 개인 정보 수정
│   │   └── query/                          # 조회 전용 서비스
│   │       └── GetUserQueryService.kt
│   ├── exception/
│   └── validator/
├── domain/
│   ├── entity/
│   ├── enums/
│   ├── port/                              # 외부 시스템 추상화 인터페이스 (Port)
│   ├── service/                           # 도메인 서비스 (여러 Entity 간 로직이 필요할 때만)
│   └── repository/
├── infrastructure/                        # Port 구현체 (Adapter)
└── presentation/
    └── *Controller.kt
```

---

## 레이어 역할 및 책임

### presentation (프레젠테이션 계층)

| 항목 | 규칙 |
|------|------|
| 역할 | HTTP 요청/응답 처리, 입력 검증 위임 |
| 포함 클래스 | `*Controller` |
| 의존 대상 | `application` 계층의 UseCase만 호출 |
| 금지 사항 | 비즈니스 로직, Entity 직접 참조, Repository 직접 참조 |

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val authUserUseCase: AuthUserUseCase,
    private val getUserQueryService: GetUserQueryService
)
```

### application (애플리케이션 계층)

| 항목 | 규칙 |
|------|------|
| 역할 | 유스케이스 흐름 조정(orchestration), 트랜잭션 관리 |
| 포함 클래스 | UseCase (command/query), Mapper, Validator, DTO, Exception |
| 의존 대상 | `domain` 계층의 Repository/Entity/Service, `infrastructure` 계층 |
| 금지 사항 | 비즈니스 규칙 직접 구현 (Entity에 위임) |

### domain (도메인 계층)

| 항목 | 규칙 |
|------|------|
| 역할 | 핵심 비즈니스 로직, 상태 변경, 검증, 데이터 무결성 |
| 포함 클래스 | Entity (Rich Domain Model), Enum, Domain Service, Repository (인터페이스) |
| 의존 대상 | 없음 (가장 안쪽 계층) |
| 금지 사항 | 다른 계층에 의존, 외부 시스템 직접 호출 |

### domain/port (포트)

| 항목 | 규칙 |
|------|------|
| 역할 | 외부 시스템에 대한 추상화 인터페이스 정의 |
| 포함 클래스 | `FileStorage`, `PushNotificationSender` 등 (인터페이스) |
| 위치 | `domain/port/` |
| 원칙 | 도메인 언어로 정의, 구현 기술에 의존하지 않음 |

### infrastructure (어댑터)

| 항목 | 규칙 |
|------|------|
| 역할 | Port 인터페이스의 구현체 (외부 시스템 연동) |
| 포함 클래스 | `S3FileStorage`, `FcmPushNotificationSender` 등 |
| 의존 대상 | `domain/port/` 인터페이스, 외부 라이브러리/SDK |
| 원칙 | 구현 기술명을 prefix로 사용 (`S3`, `Fcm`, `Redis` 등) |

---

## 의존성 규칙

```text
presentation → application → domain (port 포함)
                                ↑
                           infrastructure (port 구현)
```

- **presentation** → application만 참조
- **application** → domain(Repository/Entity/Service/Port) 참조. infrastructure를 직접 참조하지 않고 Port를 통해 사용
- **domain** → 어디에도 의존하지 않음. Port 인터페이스를 소유
- **infrastructure** → domain/port의 인터페이스를 구현. 외부 라이브러리/SDK에 의존
- **같은 도메인**: UseCase가 자기 도메인의 Repository 직접 사용
- **다른 도메인**: 해당 도메인의 QueryService를 통해 접근 (Repository 직접 참조 금지)

---

## UseCase 규칙

### Command / Query 분리

| 구분 | 패키지 | 네이밍 | 트랜잭션 |
|------|--------|--------|----------|
| Command | `usecase/command/` | `{동사}{도메인}UseCase` | `@Transactional` |
| Query | `usecase/query/` | `Get{도메인}QueryService` | `@Transactional(readOnly = true)` |

- **Command**: 상태를 변경하는 유스케이스 (생성, 수정, 삭제, 인증 등)
- **Query**: 조회만 수행하는 서비스

### UseCase 분리 기준

- 사용자 행위를 기준으로 분리
- 관련 행위는 하나의 UseCase에 묶어 파일 수를 적정하게 유지
- 예: `AuthUserUseCase` = 로그인 + 회원가입 + 탈퇴 (인증 관련 행위 묶음)

### UseCase의 책임

UseCase는 흐름을 조정(orchestrate)만 한다. Repository를 직접 사용하고, 비즈니스 로직은 Entity에 위임한다.

```kotlin
@Service
class CreatePostUseCase(
    private val postRepository: PostRepository,         // 같은 도메인 → Repository 직접 사용
    private val getUserQueryService: GetUserQueryService, // 다른 도메인 → QueryService 통해 접근
    private val fileStorage: FileStorage,               // Port 인터페이스 (구현체는 infrastructure)
    private val postMapper: PostMapper                  // application mapper
) {
    @Transactional
    fun execute(userId: Long, request: CreatePostRequest): PostResponse {
        val user = getUserQueryService.findById(userId)  // 다른 도메인은 QueryService 호출
        val imageUrl = fileStorage.upload(request.image)  // Port를 통해 외부 시스템 사용
        val post = Post.create(request.title, request.content, imageUrl, user)
        postRepository.save(post)
        return postMapper.toResponse(post)
    }
}
```

### UseCase가 하는 것 / 하지 않는 것

| 하는 것 (orchestration) | 하지 않는 것 (도메인에 위임) |
|-------------------------|---------------------------|
| Repository 호출 (조회, 저장) | 비즈니스 검증 (Entity 메서드) |
| 트랜잭션 경계 관리 | 상태 변경 로직 (Entity 메서드) |
| DTO ↔ Entity 변환 (Mapper) | 도메인 규칙 판단 |
| Port를 통한 외부 시스템 호출 | 값 계산, 정책 적용 |

---

## Entity (Rich Domain Model)

Entity가 자신의 상태를 스스로 검증하고 변경한다. 외부에서 setter로 조작하지 않는다.

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

    // 팩토리 메서드: 생성 시 검증 포함
    companion object {
        fun create(title: String, content: String, author: User): Post {
            require(title.isNotBlank()) { "제목은 비어있을 수 없습니다" }
            require(content.length <= 5000) { "본문은 5000자를 초과할 수 없습니다" }
            return Post(title = title, content = content, author = author)
        }
    }

    // 상태 변경: Entity가 자신의 규칙을 지킨다
    fun publish() {
        check(status == PostStatus.DRAFT) { "DRAFT 상태에서만 발행할 수 있습니다" }
        status = PostStatus.PUBLISHED
    }

    fun update(title: String, content: String) {
        check(status != PostStatus.DELETED) { "삭제된 게시글은 수정할 수 없습니다" }
        this.title = title
        this.content = content
    }

    fun softDelete() {
        status = PostStatus.DELETED
    }

    // 비즈니스 판단
    fun isEditableBy(userId: Long): Boolean =
        author.id == userId
}
```

### Entity 규칙

| 규칙 | 설명 |
|------|------|
| 생성 | `companion object` 팩토리 메서드 (`create`, `of`)에서 검증 포함 |
| 상태 변경 | 의미 있는 메서드명 (`publish`, `softDelete`) — setter 직접 노출 금지 |
| 검증 | `require` (인자 검증), `check` (상태 검증) 사용 |
| 판단 | `isEditableBy`, `canPublish` 등 비즈니스 판단은 Entity가 수행 |

---

## Domain Service 규칙

Domain Service는 **여러 Entity에 걸친 비즈니스 로직**이 있을 때만 생성한다.
단순 Repository 위임용 GetService/SaveService는 만들지 않는다.

### Domain Service가 필요한 경우

| 상황 | 예시 |
|------|------|
| 여러 Entity 간 협력 로직 | `TransferService` (계좌 간 이체) |
| 단일 Entity에 넣기 어려운 도메인 규칙 | `AttendancePolicy` (출석 정책 판단) |
| 외부 값이 필요한 도메인 검증 | `DuplicateCheckService` (중복 검사) |

### Domain Service가 불필요한 경우

| 상황 | 대안 |
|------|------|
| Repository.findById + 예외 처리 | UseCase에서 Repository 직접 호출 |
| Repository.save 위임 | UseCase에서 Repository 직접 호출 |
| 단일 Entity의 상태 변경 | Entity 메서드로 이동 |

```kotlin
// Domain Service 예시: 여러 Entity에 걸친 로직이 있을 때만
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

- `@Transactional`을 **붙이지 않는다** (UseCase에서 관리)
- 네이밍: 역할을 드러내는 이름 (`{역할}Service`, `{역할}Policy`)

---

## Port-Adapter 패턴

외부 시스템 의존성은 Port(인터페이스)를 domain에 정의하고, Adapter(구현체)를 infrastructure에 둔다.

### Port (domain/port/)

도메인 언어로 인터페이스를 정의한다. 구현 기술(S3, FCM 등)을 드러내지 않는다.

```kotlin
// domain/file/domain/port/FileStorage.kt
interface FileStorage {
    fun upload(file: MultipartFile): String
    fun upload(files: List<MultipartFile>): List<String>
    fun delete(fileUrl: String)
}
```

### Adapter (infrastructure/)

Port를 구현하며, 구현 기술명을 prefix로 붙인다.

```kotlin
// domain/file/infrastructure/S3FileStorage.kt
@Component
class S3FileStorage(
    private val s3Client: S3Client,
    @Value("\${cloud.aws.s3.bucket}") private val bucket: String
) : FileStorage {

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
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
    }
}
```

### Port-Adapter 네이밍

| Port (domain/port/) | Adapter (infrastructure/) |
|---------------------|--------------------------|
| `FileStorage` | `S3FileStorage` |
| `PushNotificationSender` | `FcmPushNotificationSender` |
| `CacheStore` | `RedisCacheStore` |

### 장점

- **UseCase는 Port만 알면 된다** → S3가 GCS로 바뀌어도 UseCase 변경 없음
- **테스트 시 Port를 mock** → 외부 시스템 없이 단위 테스트 가능
- **도메인이 외부 기술에 의존하지 않음** → 도메인 계층의 순수성 유지

---

## AS-IS → TO-BE 요약

| AS-IS 문제 | TO-BE 해결 |
|------------|-----------|
| UseCase가 Facade 역할, 책임 과다 | UseCase는 orchestration만, 비즈니스 로직은 Entity로 |
| GetService/SaveService가 Repository 단순 위임 | 무의미한 서비스 제거, UseCase가 Repository 직접 사용 |
| Entity가 빈 껍데기 (Anemic Domain Model) | Rich Domain Model: Entity가 검증, 상태 변경, 판단 수행 |
| 검증 로직이 UseCase/Service에 산재 | Entity의 팩토리 메서드와 상태 변경 메서드에 검증 내장 |
| UseCase가 외부 시스템 구현체에 직접 의존 | Port-Adapter로 추상화, 도메인이 Port를 소유 |

---

## 핵심 원칙

1. **Rich Domain Model**: Entity가 자신의 상태를 검증하고 변경한다. 비즈니스 규칙의 주체는 도메인이다
2. **UseCase = orchestration**: 흐름만 조정하고, "어떻게"는 Entity가 결정한다
3. **무의미한 서비스 제거**: Repository 단순 위임 서비스는 만들지 않는다. Domain Service는 여러 Entity 간 로직이 필요할 때만 생성한다
4. **Port-Adapter**: 외부 시스템은 domain/port에 인터페이스 정의, infrastructure에 구현체. 도메인이 외부 기술에 의존하지 않는다
5. **점진적 마이그레이션**: 현재 Java 구조를 최대한 유지하며 Kotlin으로 전환
