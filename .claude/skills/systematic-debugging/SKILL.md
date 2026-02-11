---
name: systematic-debugging
description: Use when encountering any bug, test failure, or unexpected behavior, before proposing fixes
---

# Debugging

Systematically debug issues using hypothesis-driven approach.
**All output MUST be written in Korean (한국어).**

## Workflow (MUST follow in order)

### 1. Collect Symptoms
- Full error message and stack trace
- Reproduction conditions (input, environment, timing)
- When it started (correlation with recent changes)
- Always vs intermittent occurrence

### 2. Form Hypotheses
List 3-5 possible causes with likelihood.
Use TaskCreate to track hypotheses.

### 3. Verify Hypotheses
In order of likelihood:
- Search and analyze related code
- Check logs/data
- Attempt reproduction with test code
- Record verification results for each

### 4. Confirm Root Cause
- Define root cause clearly
- Pinpoint exact code location
- Explain WHY this bug occurred

### 5. Fix and Verify
- Provide fix code
- Write/run test code
- Check for side effects

### 6. Prevent Recurrence
- Search for same pattern elsewhere
- Suggest preventive improvements

## Debug Checklist

### Common Bug Patterns
- NullPointerException: missing null check, unhandled Optional
- IndexOutOfBounds: empty collection, off-by-one error
- IllegalArgumentException: missing input validation
- IllegalStateException: object state mismatch
- ConcurrentModificationException: modification during iteration

### Spring/Kotlin Specific
- Bean injection failure: circular reference, conditional bean, profile
- Transaction issues: propagation, readOnly, rollback conditions
- LazyInitializationException: lazy load after session close
- Jackson serialization: circular reference, missing default constructor

### Intermittent Bugs
- Race condition: concurrency, missing locks
- Memory issues: cache expiry, GC timing
- External dependencies: API timeout, network instability
- Data-dependent: occurs only with specific data

### Environment Related
- Local vs server diff: config, env vars, resources
- Version mismatch: library, JDK, DB schema

## Useful Commands

```bash
# Recent changes
git log --oneline -20
git diff HEAD~5 -- src/

# File change history
git log -p --follow -- [filepath]

# Line author
git blame [filepath]

# Run specific test
./gradlew test --tests "*ServiceTest"
```

## Output Format

Use the following Korean template:

```markdown
# 디버깅 리포트

## 1. 증상 요약
- 에러: `UserNotFoundException` - "User not found"
- 발생 위치: `UserGetService.kt:23`
- 재현 조건: 삭제된 유저 ID로 조회 시 항상 발생

## 2. 가설 및 검증
| 가설 | 가능성 | 검증 결과 |
|------|--------|-----------|
| soft delete된 유저를 필터링하지 않음 | 높음 | ✅ 확인됨 |
| 잘못된 유저 ID 전달 | 중간 | ❌ 배제 - 로그 확인 결과 정상 ID |
| 캐시에서 만료된 데이터 조회 | 낮음 | ❌ 배제 - 캐시 미사용 |

## 3. 근본 원인
**원인**: `findById` 쿼리가 `deletedAt IS NULL` 조건을 포함하지 않아 soft delete된 유저도 조회 대상에 포함됩니다.
**위치**: `UserRepository.kt:12` - `findById` 메서드
**발생 이유**: 기본 JPA `findById`는 soft delete 필터를 적용하지 않습니다.

## 4. 수정 방안
**수정 전**:
```kotlin
fun getUser(userId: Long): User =
    userRepository.findById(userId)
        .orElseThrow { UserNotFoundException() }
```

**수정 후**:
```kotlin
fun getUser(userId: Long): User =
    userRepository.findByIdAndDeletedAtIsNull(userId)
        ?: throw UserNotFoundException()
```

**수정 이유**: soft delete 패턴에 맞게 `deletedAt IS NULL` 조건을 추가하여 삭제된 유저를 제외합니다.

## 5. 테스트
```kotlin
"soft delete된 유저 조회 시 UserNotFoundException 발생" {
    val user = UserTestFixture.createUser()
    userRepository.save(user)
    userRepository.delete(user)  // soft delete

    shouldThrow<UserNotFoundException> {
        userGetService.getUser(user.id)
    }
}
```

## 6. 재발 방지
- [x] 다른 Repository에서도 `findById` 직접 사용 여부 검사 → `FeedRepository`에서 동일 패턴 발견, 수정 완료
- [ ] `@Where(clause = "deleted_at IS NULL")` 엔티티 레벨 적용 검토
```

## Rules
- **All output in Korean (한국어)**
- Don't guess - verify with code/logs
- Form hypotheses and verify systematically
- Reproduce bug with test BEFORE fixing
- Verify test passes AFTER fixing
- Never give up until root cause is found
