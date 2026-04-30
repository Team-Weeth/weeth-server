---
name: code-review
description: "Review PR/commit code changes. Detects bugs, security vulnerabilities, performance issues and provides concrete fix suggestions."
allowed-tools: Glob, Grep, Read, Bash
---

# Code Review

Systematically review code changes, detect issues, and provide actionable fixes.
**All output MUST be written in Korean (한국어).**

## Workflow (MUST follow in order)

### 1. Analyze Changes
```bash
git diff HEAD~1 --name-only  # or git diff --staged --name-only
git diff HEAD~1              # or git diff --staged
```
- List changed files
- Assess scope and impact
- Check if related test files exist

### 2. Review by Category (in order)
1. **Critical**: Bugs, security vulnerabilities, data loss risks
2. **Major**: Performance issues, architecture violations, missing tests
3. **Minor**: Code style, naming, duplicate code
4. **Suggestion**: Better implementations, Kotlin idioms

### 3. Output Review Result
For each issue provide:
- File name and line number
- Problem description
- Severity (Critical/Major/Minor/Suggestion)
- Before/After code examples

## Review Checklist

### Bug/Logic
- Null safety (avoid "!!", use nullable types)
- Edge case handling
- Exception handling (must extend BaseException)
- Concurrency issues (race conditions)

### Security
- SQL Injection (raw queries, string concatenation)
- Sensitive data exposure (logs, responses)
- Missing auth (@CurrentUser usage)
- Input validation (@Valid, @NotNull, @NotBlank)

### Performance
- N+1 query (repository calls inside loops)
- Unnecessary DB calls
- Memory leaks (unclosed resources)

### Architecture
- Layer adherence: Controller → UseCase → Repository (UseCase uses Repository directly)
- Rich Domain Model: business logic in Entity, not UseCase
- No thin wrapper services (GetService/SaveService) — Domain Service only for multi-entity logic
- @Transactional only on UseCase methods
- Port-Adapter: UseCase depends on Port interface, not infrastructure directly
- Cross-domain read via Reader interface, cross-domain write via Repository directly
- No layer skipping (Controller → Repository is forbidden)

### Kotlin-specific
- val over var
- Nullable type overuse
- Scope function opportunities (let, apply, also)
- data class for DTOs
- when expression over if-else chains

## Output Format

Use the following Korean template:

```markdown
# 코드 리뷰 결과

## 요약
- Critical: N건
- Major: N건
- Minor: N건
- Suggestion: N건

## Critical 이슈
### [UserService.kt:42] 유저 조회 시 NPE 발생 가능
**문제**: `findById` 반환값에 대한 null 처리가 누락되어 NPE가 발생할 수 있습니다.
**수정 전**:
```kotlin
val user = userRepository.findById(userId).get()
```
**수정 후**:
```kotlin
val user = userRepository.findByIdOrNull(userId)
    ?: throw UserNotFoundException()
```

## Major 이슈
### [FeedUsecase.kt:28] N+1 쿼리 문제
**문제**: 반복문 내에서 `commentRepository.findByFeedId()`를 호출하여 N+1 쿼리가 발생합니다.
**수정 전**:
```kotlin
val feeds = feedRepository.findAll()
feeds.map { feed ->
    val comments = commentRepository.findByFeedId(feed.id)  // N+1
    feed to comments
}
```
**수정 후**:
```kotlin
val feeds = feedRepository.findAll()
val comments = commentRepository.findByFeedIdIn(feeds.map { it.id })
val commentMap = comments.groupBy { it.feedId }
feeds.map { feed -> feed to (commentMap[feed.id] ?: emptyList()) }
```

## Minor 이슈
### [UserController.kt:15] 불필요한 `var` 사용
**문제**: 재할당이 없는 변수에 `var`를 사용하고 있습니다. `val`로 변경하세요.

## Suggestion
### [UserMapper.kt:10] scope function 활용
**제안**: `also` 블록을 사용하면 로깅과 변환을 깔끔하게 분리할 수 있습니다.

## 좋은 점
- UseCase에서 트랜잭션 경계를 잘 관리하고 있습니다.
- 커스텀 예외 패턴이 일관성 있게 적용되어 있습니다.

## 전체 평가
⚠️ 수정 필요 - Critical 1건, Major 1건 수정 후 재확인 부탁드립니다.
```

## Rules
- **All output in Korean (한국어)**
- Always provide concrete fix code, not just criticism
- Praise good code when found
- Mark uncertain issues as "확인 필요"
- If no issues found, state "리뷰 완료 - 이슈 없음"
