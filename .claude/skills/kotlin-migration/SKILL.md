---
name: kotlin-migration
description: "Java → Kotlin migration skill. Follows Test-First methodology: write tests → migrate → refactor → verify with ktlint."
allowed-tools: Glob, Grep, Read, Edit, Write, Bash
---

# Kotlin Migration

Migrate Java to idiomatic Kotlin with Test-First methodology.
**All output MUST be written in Korean (한국어).**

## Batch Migration Strategy

For large-scale migrations, split work into manageable batches and get user confirmation between batches.

### Recommended Batch Units
| Scope | Batch Size | Example |
|-------|-----------|---------|
| Single Domain | 3-5 files per batch | `FeedGetService`, `FeedSaveService`, `FeedDeleteService` |
| Cross-Domain | 1 domain at a time | Complete `feed` domain before `user` domain |
| Entity + Dependencies | Entity → Repository → Services | Migrate in dependency order |

### Batch Workflow
1. **Analyze scope** - List all files to migrate
2. **Propose batch plan** - Split into logical batches, present to user
3. **Execute batch** - Migrate files in current batch
4. **Verify & Report** - Run tests, report results to user
5. **Get confirmation** - Wait for user approval before next batch
6. **Repeat** - Continue with next batch

### Example Batch Plan
```
Batch 1: Feed Entity Layer
  - Feed.java → Feed.kt
  - FeedRepository.java → FeedRepository.kt

Batch 2: Feed Service Layer
  - FeedGetService.java → FeedGetService.kt
  - FeedSaveService.java → FeedSaveService.kt
  - FeedDeleteService.java → FeedDeleteService.kt

Batch 3: Feed Application Layer
  - FeedUsecase.java → FeedUsecase.kt
  - FeedMapper.java → FeedMapper.kt
```

**Always ask user before proceeding to next batch.**

---

## Workflow (MUST follow in order)

### 1. Pre-Migration Test
- Analyze Java code behavior and dependencies
- **Write ONLY essential tests** that verify critical business logic
- Use Kotest + MockK
- Run tests against Java code to confirm they pass

**Tests to Write (HIGH value):**
- Business logic with conditions/branching
- Exception scenarios
- Complex calculations or transformations
- Transaction boundaries and side effects

**Tests to SKIP (LOW value):**
- JPA basic CRUD (findById, save, delete, findAll)
- Simple getter/setter or DTO field mapping
- Obvious pass-through methods
- Framework-provided functionality

### 2. Migration

#### File Move and Conversion
**Use `git mv` instead of delete + create to preserve history.**

```bash
git mv src/main/java/domain/{domain}/{path}/{File}.java \
       src/main/kotlin/domain/{domain}/{path}/{File}.kt
```

Then convert content from Java → Kotlin syntax using Edit tool.

```bash
./gradlew test  # Run pre-written tests
```

#### Migration Guide
- Convert preserving existing architecture patterns
- Apply Kotlin idioms: data class for DTOs, val over var, nullable only when needed
- Maintain Single Responsibility Principle
- Run tests after migration

### 3. Refactor
- Replace Java patterns with Kotlin idioms (scope functions, safe calls, when expressions)
- Run tests after each refactoring

### 4. Verify
```bash
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew test
```

## Project Patterns

### Test Style (Kotest)
**DescribeSpec** for business logic tests:
```kotlin
class UserGetServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val service = UserGetService(repository)

    describe("findById") {
        context("when user exists") {
            it("should return user") { ... }
        }
        context("when user does not exist") {
            it("should throw UserNotFoundException") { ... }
        }
    }
})
```

### Fixture Pattern
```kotlin
object UserTestFixture {
    fun createUser(
        id: Long = 1L,
        email: String = "test@example.com"
    ) = User(id = id, email = email, status = UserStatus.ACTIVE)
}
```
Location: `src/test/kotlin/{domain}/test/fixture/`

## Output Format

Use the following Korean template for reporting:

```markdown
# 마이그레이션 리포트

## 대상 파일
| 파일 | 상태 | 비고 |
|------|------|------|
| `FeedGetService.java` → `.kt` | ✅ 완료 | 테스트 3건 통과 |
| `FeedSaveService.java` → `.kt` | ✅ 완료 | 테스트 1건 통과 |
| `FeedDeleteService.java` → `.kt` | ✅ 완료 | 테스트 불필요 (단순 위임) |

## 작성된 테스트
- `FeedGetServiceTest.kt`: 3건 (존재하지 않는 피드 조회, 차단된 사용자 접근, 정상 조회)
- `FeedSaveServiceTest.kt`: 1건 (중복 피드 생성 방지)

## 주요 변환 사항
- `Optional.orElseThrow()` → `?: throw` 패턴 적용
- MapStruct → 수동 Mapper 패턴으로 전환
- Lombok 제거, Kotlin 생성자 주입 적용

## 검증 결과
- ktlintCheck: ✅ 통과
- 전체 테스트: ✅ 통과 (N건)

## 다음 배치
Batch 3: Feed Application Layer (FeedUsecase, FeedMapper) 진행할까요?
```

## Rules
- **All output in Korean (한국어)**
- Never skip tests
- Never migrate without passing tests first
- Fix Kotlin code if tests fail (not tests)
- Always use `git mv` for file moves
- Ask user before proceeding to next batch
