# 동아리 탈퇴 / 위드 탈퇴 / 동아리 삭제 구현 계획

> **본 문서는 상위 설계 문서**입니다. 실제 정책과 데이터 모델 변경은 하위 이슈 문서가 우선합니다.
> - [WTH-390 동아리 탈퇴](./WTH-390-club-leave.md) — 멤버 종속 데이터에 메타데이터를 추가하지 않고 `ClubMember` 상태 기반 조회 분기로 단순화한 정책 채택. 후속 이슈도 이 방향을 따른다.

## 목적

다음 3개 기능을 일관된 보관 정책으로 제공한다.

| 기능 | 설명 | 권한 |
|---|---|---|
| 동아리 탈퇴 | 사용자가 특정 동아리에서만 탈퇴하고, 해당 동아리에서 활동한 내역을 삭제 예약한다. | 해당 동아리 ACTIVE 멤버 |
| 위드 탈퇴 | 사용자가 서비스에서 탈퇴하고, 모든 동아리 활동 내역을 삭제 예약한다. | 로그인 사용자 |
| 동아리 삭제 | 동아리 자체와 동아리 하위 데이터를 삭제 예약한다. | 해당 동아리 LEAD |

삭제 정책은 공통으로 적용한다.

- 사용자에게는 즉시 보이지 않도록 soft delete 처리한다.
- soft delete 시점부터 30일 뒤 hard delete한다.
- 30일 보관은 복구 기능 제공이 아니라 지연 삭제와 운영 대응을 위한 보관 기간으로 본다.

## 현재 상태

### 이미 존재하는 상태 전이

| 대상 | 현재 필드 / 메서드 | 위치 |
|---|---|---|
| 사용자 | `User.status = LEFT`, `User.leave()` | `user/domain/entity/User.kt` |
| 동아리 멤버 | `ClubMember.memberStatus = LEFT`, `ClubMember.leave()` | `club/domain/entity/ClubMember.kt` |
| 게시글 | `Post.isDeleted`, `Post.markDeleted()` | `board/domain/entity/Post.kt` |
| 댓글 | `Comment.isDeleted`, `Comment.markAsDeleted()` | `comment/domain/entity/Comment.kt` |
| 게시판 | `Board.isDeleted`, `Board.markDeleted()` | `board/domain/entity/Board.kt` |

### 부족한 점

| 항목 | 문제 |
|---|---|
| 삭제 시각 | 대부분의 엔티티에 `deletedAt`, `hardDeleteAfter`가 없어 30일 뒤 hard delete 대상을 판단할 수 없다. |
| 활동 내역 soft delete | `Attendance`, `Penalty`, `ClubMemberCardinal`, `PostLike`, `LastNoticeRead` 등은 soft delete 필드가 없다. |
| 동아리 삭제 | `Club`에 삭제 상태가 없다. 현재 TODO만 존재한다. |
| 위드 탈퇴 | `AuthUserUseCase.leave()`는 사용자 상태만 `LEFT`로 변경하고 동아리 활동 내역을 처리하지 않는다. |
| hard delete 배치 | 스케줄러는 출석 자동 마감만 있고 삭제 정리 배치는 없다. |

## 용어와 범위

### 활동 내역

동아리 탈퇴 시 삭제 예약할 활동 내역은 `ClubMember`를 기준으로 추적 가능한 데이터로 정의한다.

| 범위 | 대상 |
|---|---|
| 멤버십 | `club_member`, `club_member_cardinal` |
| 출석 / 벌점 | `attendance`, `penalty` |
| 게시판 활동 | 내가 작성한 `post`, `comment`, 내가 누른 `post_like`, `last_notice_read` |
| 파일 | `CLUB_MEMBER_PROFILE`, 내가 작성한 `POST`, `COMMENT` 첨부 파일 |

동아리 삭제 시에는 위 멤버별 활동 내역에 더해 동아리 소유 데이터를 삭제 예약한다.

| 범위 | 대상 |
|---|---|
| 동아리 | `club`, `cardinal`, `board` |
| 일정 / 출석 | `session`, `session_group`, `event`, `attendance` |
| 회계 | `account`, `receipt` |
| 파일 | `CLUB_PROFILE`, `CLUB_BACKGROUND`, 동아리 하위 게시글/댓글/영수증 파일 |

## 권한 정책

### 동아리 탈퇴

- `ClubMemberPolicy.getActiveMemberWithLock(clubId, userId)`로 본인 멤버십을 잠근다.
- LEAD는 일반 탈퇴를 허용하지 않는다.
- LEAD가 동아리를 나가려면 먼저 LEAD를 이양하거나 동아리 삭제를 수행해야 한다.
- 탈퇴 후 `ClubMember.memberStatus`는 `LEFT`가 된다.

### 위드 탈퇴

- `User`를 `PESSIMISTIC_WRITE`로 잠근다.
- 사용자의 모든 ACTIVE 동아리 멤버십을 조회한다.
- ACTIVE LEAD 멤버십이 하나라도 있으면 위드 탈퇴를 차단한다.
- 차단 이유: 위드 탈퇴가 동아리 전체 삭제를 암묵적으로 수행하면 다른 멤버 데이터까지 삭제된다.
- 사용자가 LEAD인 동아리는 권한 이양 또는 명시적 동아리 삭제 후 다시 위드 탈퇴해야 한다.
- 통과하면 모든 ACTIVE 멤버십에 동아리 탈퇴와 동일한 삭제 예약을 적용한 뒤 `User.leave()`를 호출한다.

### 동아리 삭제

- `ClubPermissionPolicy.requireLead(clubId, userId)`를 추가하거나 기존 정책에 LEAD 전용 검증 메서드를 추가한다.
- ADMIN은 삭제할 수 없다.
- 동아리를 잠근 뒤 동아리 하위 전체 데이터를 soft delete한다.
- 동아리 삭제 후 해당 동아리의 모든 멤버는 조회/접근 불가능해야 한다.

## 데이터 모델 변경안

### 공통 삭제 메타데이터

soft delete와 hard delete 예약이 필요한 엔티티에 다음 필드를 추가한다.

```kotlin
@Column(nullable = false)
var isDeleted: Boolean = false
    private set

var deletedAt: LocalDateTime? = null
    private set

var hardDeleteAfter: LocalDateTime? = null
    private set
```

이미 `isDeleted`가 있는 엔티티는 `deletedAt`, `hardDeleteAfter`만 추가한다.

```kotlin
fun markDeleted(now: LocalDateTime) {
    if (isDeleted) return
    isDeleted = true
    deletedAt = now
    hardDeleteAfter = now.plusDays(30)
}
```

상태 enum을 사용하는 엔티티는 기존 상태를 유지하되 삭제 메타데이터를 추가한다.

| 엔티티 | 변경 |
|---|---|
| `User` | `leftAt`, `hardDeleteAfter` 추가. `leave(now)`로 변경 |
| `Club` | `isDeleted`, `deletedAt`, `hardDeleteAfter`, `delete(now)` 추가 |
| `ClubMember` | `leftAt`, `hardDeleteAfter` 추가. `leave(now)`로 변경 |
| `ClubMemberCardinal` | `isDeleted`, `deletedAt`, `hardDeleteAfter` 추가 |
| `Attendance` | `isDeleted`, `deletedAt`, `hardDeleteAfter` 추가 |
| `Penalty` | `isDeleted`, `deletedAt`, `hardDeleteAfter` 추가 |
| `Post` | `deletedAt`, `hardDeleteAfter` 추가 |
| `Comment` | `deletedAt`, `hardDeleteAfter` 추가 |
| `PostLike` | `deletedAt`, `hardDeleteAfter` 추가. `isActive=false`도 함께 처리 |
| `LastNoticeRead` | `isDeleted`, `deletedAt`, `hardDeleteAfter` 추가 |
| `Board` | `deletedAt`, `hardDeleteAfter` 추가 |
| `Session`, `SessionGroup`, `Event`, `Cardinal`, `Account`, `Receipt`, `File` | 동아리 삭제용 `isDeleted`, `deletedAt`, `hardDeleteAfter` 추가 검토 |

### 인덱스

hard delete 배치 성능을 위해 각 삭제 대상 테이블에 인덱스를 추가한다.

```sql
CREATE INDEX idx_{table}_deleted_after ON {table}(is_deleted, hard_delete_after);
```

`User`, `ClubMember`처럼 status 기반으로 삭제를 표현하는 테이블은 다음 형태를 사용한다.

```sql
CREATE INDEX idx_club_member_status_hard_delete_after ON club_member(member_status, hard_delete_after);
CREATE INDEX idx_users_status_hard_delete_after ON users(status, hard_delete_after);
```

## API 설계

### 동아리 탈퇴

기존 API를 확장한다.

```http
DELETE /api/v4/clubs/{clubId}/leave
```

응답:

```kotlin
CommonResponse.success(ClubResponseCode.CLUB_LEFT_SUCCESS)
```

주요 에러:

| 에러 | HTTP | 설명 |
|---|---|---|
| `CLUB_MEMBER_NOT_FOUND` | 404 | 동아리 멤버가 아님 |
| `MEMBER_NOT_ACTIVE` | 403 | ACTIVE 멤버가 아님 |
| `CANNOT_LEAVE_AS_LEAD` | 409 | LEAD는 일반 탈퇴 불가 |

### 위드 탈퇴

신규 API를 추가한다.

```http
DELETE /api/v4/users/me
```

응답 코드 추가:

| 코드 | HTTP | 메시지 |
|---|---|---|
| `USER_LEFT_SUCCESS` | 10906 | 위드 탈퇴가 완료되었습니다. |

에러 코드 추가:

| 코드 | HTTP | 메시지 |
|---|---|---|
| `USER_HAS_LEAD_CLUB` | 209xx | LEAD인 동아리가 있어 탈퇴할 수 없습니다. |

### 동아리 삭제

관리자 API에 추가한다.

```http
DELETE /api/v4/admin/clubs/{clubId}
```

응답 코드 추가:

| 코드 | HTTP | 메시지 |
|---|---|---|
| `CLUB_DELETED_SUCCESS` | 11125 | 동아리가 삭제되었습니다. |

에러 코드 추가:

| 코드 | HTTP | 메시지 |
|---|---|---|
| `CLUB_DELETE_ONLY_LEAD` | 211xx | 동아리 삭제는 LEAD만 할 수 있습니다. |

## UseCase 설계

### 공통 삭제 정책 서비스

초기 구현은 단일 도메인 서비스로 시작한다. 동아리 탈퇴, 위드 탈퇴, 동아리 삭제가 모두 같은 보관 기간과 삭제 예약 규칙을 사용하므로, 별도 핸들러 구조를 먼저 만들 필요는 없다.

```text
club/domain/service/ClubActivityDeletionPolicy.kt
```

역할:

- `ClubMember` 기준 활동 내역을 soft delete한다.
- soft delete 시각과 hard delete 예정 시각을 동일하게 계산한다.
- 여러 도메인 엔티티를 함께 다루는 삭제 예약 정책을 한 곳에서 조율한다.
- 추후 회비 등 삭제 대상 기능이 추가되면 우선 이 정책에 필요한 Repository와 삭제 예약 로직을 추가한다.
- 정책 클래스가 과도하게 커지거나 도메인별 삭제 규칙이 명확히 갈라질 때만 도메인별 핸들러 분리를 검토한다.

메서드 예시:

```kotlin
fun markMemberActivitiesDeleted(
    clubMember: ClubMember,
    now: LocalDateTime,
)

fun markClubActivitiesDeleted(
    club: Club,
    now: LocalDateTime,
)
```

### 동아리 탈퇴

파일:

```text
club/application/usecase/command/ManageClubMemberUsecase.kt
```

흐름:

1. `ClubMember`를 lock으로 조회한다.
2. LEAD인지 검증한다.
3. `ClubActivityDeletionPolicy.markMemberActivitiesDeleted(member, now)` 호출한다.
4. `member.leave(now)` 호출한다.

### 위드 탈퇴

파일:

```text
user/application/usecase/command/AuthUserUseCase.kt
```

흐름:

1. `User`를 lock으로 조회한다.
2. ACTIVE 멤버십 전체를 lock으로 조회한다.
3. LEAD 멤버십이 있으면 `UserHasLeadClubException`을 던진다.
4. 각 멤버십에 `markMemberActivitiesDeleted(member, now)`를 적용한다.
5. 각 멤버십 `leave(now)`를 호출한다.
6. `user.leave(now)`를 호출한다.
7. refresh token / 쿠키 무효화가 있다면 함께 처리한다.

### 동아리 삭제

파일:

```text
club/application/usecase/command/ManageClubUseCase.kt
```

흐름:

1. `ClubPermissionPolicy.requireLead(clubId, userId)`로 LEAD만 허용한다.
2. `Club`을 lock으로 조회한다.
3. `ClubActivityDeletionPolicy.markClubActivitiesDeleted(club, now)` 호출한다.
4. 모든 ACTIVE 멤버십을 `LEFT`로 변경하거나, 별도 `DELETED` 상태 추가를 검토한다.
5. `club.delete(now)`를 호출한다.

## Repository 추가 메서드

### ClubMemberRepository

```kotlin
fun findAllByUserIdAndMemberStatusWithLock(userId: Long, status: MemberStatus): List<ClubMember>
fun findAllByClubIdWithLock(clubId: Long): List<ClubMember>
```

### 활동 내역 Repository

각 Repository에 멤버 또는 동아리 기준 조회/벌크 soft delete 메서드를 추가한다.

| Repository | 필요 메서드 |
|---|---|
| `AttendanceRepository` | `findAllByClubMemberIn(...)`, `findAllByClubId(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `PenaltyRepository` | `findAllByClubMemberIn(...)`, `findAllByClubId(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `PostRepository` | `findAllByClubMemberIn(...)`, `findAllByClubId(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `CommentRepository` | `findAllByClubMemberIn(...)`, `findAllByClubId(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `PostLikeRepository` | `findAllByUserId(...)`, `findAllByPostIn(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `LastNoticeReadRepository` | `findAllByUserId(...)`, `findAllByBoardClubId(...)`, `deleteHardByHardDeleteAfterBefore(...)` |
| `FileRepository` | owner type/id 기준 soft delete, hard delete 대상 조회 |

벌크 update를 사용할 경우 영속성 컨텍스트 불일치를 피하기 위해 `@Modifying(clearAutomatically = true, flushAutomatically = true)`를 사용한다.

## 조회 로직 변경

soft delete 도입 후 모든 사용자 조회는 삭제 데이터를 제외해야 한다.

| 영역 | 변경 |
|---|---|
| 동아리 조회 | `club.isDeleted = false` 조건 추가 |
| 멤버 조회 | `memberStatus = ACTIVE` 유지, 삭제 예정 멤버 제외 |
| 게시글 / 댓글 조회 | 기존 `isDeleted=false` 조건 유지, `deletedAt is null` 또는 `hardDeleteAfter is null` 조건 일관화 |
| 출석 / 벌점 조회 | `isDeleted=false` 조건 추가 |
| 파일 조회 | `isDeleted=false` 또는 `status=UPLOADED` 정책과 통합 |

권장 기준:

- 화면 조회 조건은 `isDeleted=false` 또는 상태가 `ACTIVE`인 데이터만 노출한다.
- hard delete 예정 데이터는 운영/배치 외에는 조회하지 않는다.

## Hard Delete 배치

### 위치

```text
global/deletion/infrastructure/DeletionCleanupScheduler.kt
global/deletion/application/usecase/DeleteExpiredSoftDeletedDataUseCase.kt
```

삭제는 여러 도메인에 걸치므로 `global/deletion` 또는 별도 `domain/deletion` 패키지를 둔다.

### 스케줄

```kotlin
@Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
fun cleanupExpiredSoftDeletedData()
```

### 삭제 순서

FK 제약을 고려해 하위 데이터부터 삭제한다.

1. `file`
2. `post_like`, `last_notice_read`
3. `comment`
4. `post`
5. `attendance`
6. `penalty`
7. `club_member_cardinal`
8. `receipt`
9. `account`
10. `session`
11. `session_group`
12. `event`
13. `board`
14. `cardinal`
15. `club_member`
16. `club`
17. `user_social_account`
18. `users`

실제 FK 방향을 기준으로 테스트에서 삭제 순서를 검증한다.

### 파일 스토리지

DB hard delete와 외부 스토리지 객체 삭제는 같은 트랜잭션으로 묶을 수 없다.

권장:

- soft delete 시 DB의 `File`만 삭제 예약한다.
- hard delete 배치에서 먼저 스토리지 삭제를 시도한다.
- 스토리지 삭제 성공 후 DB `File`을 hard delete한다.
- 스토리지 삭제 실패 시 DB row는 유지하고 다음 배치에서 재시도한다.

## 테스트 계획

### 단위 테스트

| 대상 | 케이스 |
|---|---|
| `ClubMember.leave(now)` | ACTIVE만 LEFT 전환, `leftAt`, `hardDeleteAfter` 설정 |
| `User.leave(now)` | LEFT 전환, 삭제 예정 시각 설정 |
| `Club.delete(now)` | 삭제 상태와 삭제 예정 시각 설정 |
| `ClubPermissionPolicy.requireLead` | LEAD 통과, ADMIN/USER 실패 |

### UseCase 테스트

| 대상 | 케이스 |
|---|---|
| 동아리 탈퇴 | 일반 멤버 탈퇴 성공, LEAD 탈퇴 실패, 활동 내역 soft delete |
| 위드 탈퇴 | 일반 멤버십 전체 탈퇴 성공, LEAD 멤버십 보유 시 실패 |
| 동아리 삭제 | LEAD 성공, ADMIN 실패, 동아리 하위 데이터 soft delete |
| hard delete 배치 | 30일 미만 데이터 유지, 30일 지난 데이터 삭제, FK 순서 검증 |

### 통합 테스트

- Testcontainers MySQL로 FK 삭제 순서를 검증한다.
- 탈퇴 후 게시글/댓글/출석/벌점 조회 API에서 삭제 데이터가 보이지 않는지 검증한다.
- 동아리 삭제 후 클럽 상세, 게시판, 일정, 출석 API 접근이 실패하는지 검증한다.

## 구현 순서

1. 삭제 정책 확정: 위드 탈퇴 시 LEAD 보유 사용자를 차단하는지 최종 확인한다.
2. 엔티티 삭제 메타데이터 추가: `User`, `Club`, `ClubMember`부터 시작한다.
3. 활동 엔티티 삭제 메타데이터 추가: 출석, 벌점, 게시글, 댓글, 좋아요, 파일.
4. Repository 조회 조건과 삭제 대상 조회 메서드 추가.
5. `ClubActivityDeletionPolicy` 추가.
6. 동아리 탈퇴 UseCase 확장.
7. 위드 탈퇴 API와 UseCase 확장.
8. 동아리 삭제 API와 UseCase 추가.
9. hard delete 배치 추가.
10. 테스트 작성 및 `ktlintFormat`, 관련 테스트 실행.

## 결정 필요 사항

| 항목 | 권장안 |
|---|---|
| 위드 탈퇴 사용자가 LEAD인 경우 | 자동 동아리 삭제 금지. 권한 이양 또는 명시적 동아리 삭제 후 탈퇴 |
| 30일 내 복구 API 제공 여부 | 이번 범위에서 제외 |
| 댓글 표시 방식 | 내 댓글은 즉시 `삭제된 댓글입니다.`로 치환하고 30일 뒤 hard delete |
| 게시글 파일 / 댓글 파일 | soft delete 시 함께 삭제 예약 |
| 회계 데이터 | 동아리 삭제에만 포함. 개인 탈퇴로는 삭제하지 않음 |
| hard delete 실패 재시도 | 삭제 예정 row를 유지하고 다음 배치에서 재시도 |
| 삭제 정책 확장 방식 | 초기에는 `ClubActivityDeletionPolicy` 단일 클래스로 유지. 회비 등 기능 추가 시 먼저 해당 정책에 로직을 추가하고, 비대해질 때만 분리 검토 |
