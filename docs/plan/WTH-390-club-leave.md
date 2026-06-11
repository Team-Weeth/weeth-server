# [WTH-390] 동아리 탈퇴 구현 계획

상위 플랜: [withdrawal-and-club-deletion.md](./withdrawal-and-club-deletion.md)

## 목적

사용자가 특정 동아리에서만 탈퇴하고, 해당 동아리에서 활동한 내역을 일관된 보관 정책으로 처리한다.

- soft delete 즉시 적용 → 일반 사용자에게는 보이지 않음
- soft delete 시점 + 30일에 hard delete 예정 (배치는 본 이슈 범위 외)
- 30일 보관은 운영 대응 및 향후 복구 가능성을 위한 보관 기간

## 범위

본 이슈는 **멤버 축 (한 멤버 + 그 멤버의 활동 내역)** 만 다룬다. 위드 탈퇴, 동아리 삭제, hard delete 배치는 후속 이슈에서 처리한다.

### In scope

| 영역 | 내용 |
|---|---|
| API | `DELETE /api/v4/clubs/{clubId}/leave` 동작 확장 |
| 엔티티 메타데이터 | `ClubMember`(`leftAt`, `hardDeleteAfter`), `PostLike`(`deletedAt`), `File`(`CLUB_MEMBER_PROFILE`만 soft delete) |
| 조회 정책 | `Post`/`Comment` 매퍼 작성자 익명화. 출석/벌점/멤버 등 ACTIVE 필터를 일반 경로에 한해 적용 |
| 정책 서비스 | `ClubActivityDeletionPolicy.markMemberActivitiesDeleted` 신규 |

### Out of scope (다른 이슈)

- 위드 탈퇴 (`DELETE /api/v4/users/me`)
- 동아리 삭제 (`DELETE /api/v4/admin/clubs/{clubId}`)
- 동아리 소유 엔티티(`Club`, `Cardinal`, `Board`, `Session`, `Event`, `Account`, `Receipt`) 메타데이터
- hard delete 배치 (`DeletionCleanupScheduler`)
- 외부 스토리지 2-phase 삭제
- 30일 내 복구 API

## 권한 정책

- `ClubMemberPolicy.getActiveMemberWithLock(clubId, userId)`로 본인 멤버십을 lock 조회
- LEAD는 일반 탈퇴 불가 → `CANNOT_LEAVE_AS_LEAD` 반환
- 탈퇴 후 `ClubMember.memberStatus = LEFT`

## 핵심 설계 원칙

**`ClubMember` 종속 데이터는 엔티티 자체에 메타데이터를 추가하지 않는다.**

- hard delete 시점은 `ClubMember.hardDeleteAfter` 한 곳에서 관리. 종속 엔티티는 30일 뒤 cascade로 정리.
- 조회 분기는 종속 엔티티의 `isDeleted` 컬럼이 아니라 `ClubMember.memberStatus` / `User.status` 상태로 결정.
- 게시글/댓글은 다른 멤버의 스레드 맥락을 깨지 않도록 본문 보존 + 작성자 익명화.
- 출석/벌점은 동아리 운영 기록이므로 관리자 경로에서는 LEFT 멤버 데이터도 함께 보인다.

이 원칙 덕분에 본 이슈에서 컬럼 추가가 필요한 엔티티는 `ClubMember`, `PostLike`, `File` 세 개로 축소된다.

## 엔티티별 처리 정책

"30일 후" 컬럼은 모두 후속 hard delete 배치 이슈에서 처리한다. 본 이슈는 hard delete 자체를 다루지 않는다.

| 엔티티 | 탈퇴 즉시 | 일반 경로 조회 | 관리자 경로 조회 | 30일 후 (배치 이슈) |
|---|---|---|---|---|
| `ClubMember` | `LEFT` + `leftAt` + `hardDeleteAfter` | 멤버 목록 제외 | LEFT 포함 | hard delete |
| `Attendance` | 변경 없음 | `member.status = ACTIVE` 필터 | 필터 없음 | `ClubMember`와 동반 삭제 |
| `Penalty` | 변경 없음 | `member.status = ACTIVE` 필터 | 필터 없음 | `ClubMember`와 동반 삭제 |
| `ClubMemberCardinal` | 변경 없음 | 멤버 따라감 | 멤버 따라감 | `ClubMember`와 동반 삭제 |
| `Post` | 변경 없음 | 본문 노출 + 작성자 익명화 | 작성자 익명화(또는 원본 표시 — 정책에 따름) | `ClubMember`와 동반 삭제 |
| `Comment` | 변경 없음 | 본문 노출 + 작성자 익명화 | 작성자 익명화(또는 원본 표시) | `ClubMember`와 동반 삭제 |
| `PostLike` | `post.decreaseLikeCount()` + `postLike.markDeleted(now)`(`isActive=false`, `deletedAt`) | `deletedAt IS NULL` 한정 | 동일 | `ClubMember`와 동반 삭제 |
| `LastNoticeRead` | 변경 없음 | 본인만 보는 데이터 — 영향 없음 | — | `ClubMember`와 동반 삭제 |
| `File` (`POST`, `COMMENT` 첨부) | 변경 없음 | 본문 따라 보존 | 동일 | `ClubMember`와 동반 삭제 |
| `File` (`CLUB_MEMBER_PROFILE`) | `markDeleted(now)` | 익명 처리 위해 가림 | 동일 | hard delete |

## 데이터 모델 변경

### `ClubMember`

```kotlin
var leftAt: LocalDateTime? = null
    private set

var hardDeleteAfter: LocalDateTime? = null
    private set

fun leave(now: LocalDateTime) {
    check(memberStatus == MemberStatus.ACTIVE) { "ACTIVE 상태에서만 탈퇴할 수 있습니다" }
    memberStatus = MemberStatus.LEFT
    leftAt = now
    hardDeleteAfter = now.plusDays(30)
}
```

기존 `leave()` 시그니처는 `leave(now)`로 변경. 호출부 일괄 수정.

### `PostLike`

```kotlin
var deletedAt: LocalDateTime? = null
    private set

fun markDeleted(now: LocalDateTime) {
    if (deletedAt != null) return
    isActive = false
    deletedAt = now
}

fun restore() {
    deletedAt = null
    isActive = true
}
```

조회 쿼리는 `deletedAt IS NULL` 조건 추가. `restore()`는 본 이슈에서 메서드와 단위 테스트까지만 추가하고 호출처는 후속 복구 이슈에서 연결한다.

### `File`

`CLUB_MEMBER_PROFILE` owner에 한해 `markDeleted(now)` 적용. 기존 `isDeleted` 컬럼이 있으면 `deletedAt`만 추가, 없으면 함께 추가.

### 마이그레이션

```sql
ALTER TABLE club_member
    ADD COLUMN left_at DATETIME(6) NULL,
    ADD COLUMN hard_delete_after DATETIME(6) NULL;

ALTER TABLE post_like
    ADD COLUMN deleted_at DATETIME(6) NULL;

-- file 테이블이 이미 is_deleted를 가질 경우
ALTER TABLE file
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN hard_delete_after DATETIME(6) NULL;
```

### 인덱스

hard delete 배치 성능을 위해 본 이슈에서 함께 추가한다.

```sql
CREATE INDEX idx_club_member_status_hard_delete_after
    ON club_member(member_status, hard_delete_after);

CREATE INDEX idx_post_like_deleted_at
    ON post_like(deleted_at);

CREATE INDEX idx_file_deleted_after
    ON file(is_deleted, hard_delete_after);
```

## API

### 엔드포인트

```http
DELETE /api/v4/clubs/{clubId}/leave
```

요청 본문 없음. `@CurrentUser`로 사용자 ID 주입.

### 응답

```kotlin
CommonResponse.success(ClubResponseCode.CLUB_LEFT_SUCCESS)
```

### 에러 코드

| 코드 | HTTP | 메시지 | 시나리오 |
|---|---|---|---|
| `CLUB_MEMBER_NOT_FOUND` | 404 | 동아리 멤버가 아닙니다 | 멤버십이 존재하지 않음 |
| `MEMBER_NOT_ACTIVE` | 403 | ACTIVE 상태가 아닙니다 | 이미 LEFT/PENDING |
| `CANNOT_LEAVE_AS_LEAD` (신규) | 409 | LEAD는 일반 탈퇴를 할 수 없습니다 | LEAD 권한 보유 |

`ClubErrorCode`에 `CANNOT_LEAVE_AS_LEAD`만 신규 추가하고, 나머지는 기존 enum을 재사용한다.

## UseCase 설계

### 파일

```text
domain/club/application/usecase/command/ManageClubMemberUsecase.kt
```

### 흐름

```kotlin
@Transactional
fun leave(clubId: Long, userId: Long) {
    val now = LocalDateTime.now()
    val member = clubMemberPolicy.getActiveMemberWithLock(clubId, userId)

    if (member.role == MemberRole.LEAD) {
        throw CannotLeaveAsLeadException()
    }

    clubActivityDeletionPolicy.markMemberActivitiesDeleted(member, now)
    member.leave(now)
}
```

### 정책 서비스

```text
domain/club/domain/service/ClubActivityDeletionPolicy.kt
```

```kotlin
@Service
class ClubActivityDeletionPolicy(
    private val postLikeRepository: PostLikeRepository,
    private val fileRepository: FileRepository,
) {
    fun markMemberActivitiesDeleted(member: ClubMember, now: LocalDateTime) {
        deactivatePostLikes(member, now)
        softDeleteMemberProfileFile(member, now)
    }

    private fun deactivatePostLikes(member: ClubMember, now: LocalDateTime) {
        val likes = postLikeRepository.findAllActiveByUserIdAndClubId(
            userId = member.user.id,
            clubId = member.club.id,
        )
        likes.forEach { like ->
            like.markDeleted(now)
            // 좋아요 카운트 정합성을 즉시 맞춤. 30일 내 복구 시 increase로 되돌림.
            like.post.decreaseLikeCount()
        }
    }

    private fun softDeleteMemberProfileFile(member: ClubMember, now: LocalDateTime) {
        fileRepository.findAllByOwner(FileOwnerType.CLUB_MEMBER_PROFILE, member.id)
            .forEach { it.markDeleted(now) }
    }
}
```

- `@Transactional`은 UseCase에만, Policy에는 붙이지 않는다.
- 위드 탈퇴 이슈에서 그대로 재사용할 수 있도록 시그니처는 멤버 한 명 단위로 고정한다.

## 조회 로직 변경

본 이슈의 조회 정책 핵심은 **"엔티티의 `isDeleted` 컬럼을 보지 않고 멤버 상태를 본다"** 이다.

### 일반 사용자 경로

| 영역 | 변경 |
|---|---|
| 출석 조회 | `clubMember.memberStatus = ACTIVE` 필터를 join에 추가 |
| 벌점 조회 | `clubMember.memberStatus = ACTIVE` 필터를 join에 추가 |
| 멤버 목록 | 기존 ACTIVE 필터 유지 |
| 게시글/댓글 조회 | 본문 노출 정책 유지. 매퍼에서 작성자 익명화 |
| 좋아요 조회 | `postLike.deletedAt IS NULL` 필터 추가 |

### 관리자 경로

- 출석부, 벌점 관리 등 운영 화면은 LEFT 멤버를 포함한다.
- 별도 admin Reader/Repository 메서드를 추가하거나, 기존 admin 쿼리에서 `member.status` 필터를 적용하지 않는 방식으로 분리한다.
- 본 이슈 작업 항목에 **현재 admin 경로 쿼리 목록을 식별하고 ACTIVE 필터가 들어있지 않은지 검증**하는 작업이 포함된다.

### 매퍼 익명화 (`PostMapper`, `CommentMapper`)

```kotlin
fun toResponse(post: Post): PostResponse {
    val author = post.clubMember
    val isAuthorLeft = author.memberStatus == MemberStatus.LEFT
    return PostResponse(
        // ...
        authorName = if (isAuthorLeft) "탈퇴한 사용자" else author.user.name,
        authorProfileUrl = if (isAuthorLeft) DEFAULT_PROFILE_URL else author.user.profileUrl,
        // ...
    )
}
```

- 라벨 문구: `"탈퇴한 사용자"`
- 프로필 이미지: 기본 이미지 URL (상수로 분리)
- 본문, 좋아요 수, 댓글 수, 생성 시각은 그대로
- 관리자 화면에서 원본을 노출할지 여부는 후속 결정. 기본 동작은 일반 경로와 동일하게 익명화.

## Repository 추가 메서드

| Repository | 추가 메서드 |
|---|---|
| `PostLikeRepository` | `findAllActiveByUserIdAndClubId(userId: Long, clubId: Long): List<PostLike>` |
| `FileRepository` | `findAllByOwner(ownerType: FileOwnerType, ownerId: Long): List<File>` |

출석/벌점/게시글/댓글에는 별도 조회 메서드를 추가하지 않는다. 기존 조회 경로에 `member.status` 필터 / 매퍼 익명화만 적용.

## 테스트 계획

### 단위 테스트

| 대상 | 케이스 |
|---|---|
| `ClubMember.leave(now)` | ACTIVE → LEFT, `leftAt`/`hardDeleteAfter` 설정. LEFT 재호출 시 `check` 실패 |
| `PostLike.markDeleted(now)` | `isActive=false`, `deletedAt` 설정. 재호출 시 멱등 |
| `PostLike.restore()` | `isActive=true`, `deletedAt=null` |

### UseCase 테스트 (`ManageClubMemberUsecaseTest`)

| 케이스 | 기대 |
|---|---|
| ACTIVE 일반 멤버 탈퇴 | `member.leave(now)` 호출, `markMemberActivitiesDeleted` 1회 호출 |
| LEAD 탈퇴 시도 | `CannotLeaveAsLeadException` |
| 이미 LEFT 멤버 탈퇴 시도 | `MemberNotActiveException` |
| 멤버십 없음 | `ClubMemberNotFoundException` |

### Policy 테스트 (`ClubActivityDeletionPolicyTest`)

| 케이스 | 기대 |
|---|---|
| 활성 좋아요가 있는 멤버 | 각 `PostLike.markDeleted` 호출 + `Post.likeCount` 차감 |
| 좋아요 없는 멤버 | 통과 |
| 프로필 파일이 있는 멤버 | `File.markDeleted` 호출 |
| 다른 동아리 좋아요는 영향 없음 | 다른 동아리 PostLike는 그대로 유지 |

### 통합 테스트

Testcontainers MySQL 기준. 탈퇴 후 다음을 검증:

- `GET /clubs/{clubId}/members` 응답에 탈퇴자 미포함
- `GET /clubs/{clubId}/boards/{boardId}/posts` 응답에서 탈퇴자 게시글 본문이 보이되 `authorName == "탈퇴한 사용자"`
- 일반 출석 조회 API에서 탈퇴자 출석 미포함
- 관리자 출석 조회 API에서 탈퇴자 출석 포함
- 탈퇴자가 누른 게시글의 `likeCount`가 즉시 감소
- DB에서 `club_member.left_at`, `hard_delete_after` 30일 뒤 설정 확인
- 탈퇴자가 누른 `post_like` row의 `deleted_at` 설정 확인 (hard delete 안 됨)

## 구현 순서

1. **`ClubMember` 메타데이터 + 마이그레이션** (PR1)
   - `leftAt`, `hardDeleteAfter` 추가, `leave(now)` 시그니처 변경, 기존 호출부 수정
   - 인덱스 생성
   - 단위 테스트
2. **조회 필터 및 매퍼 익명화** (PR2)
   - 일반 경로 출석/벌점 쿼리에 `member.status = ACTIVE` 필터 적용
   - 관리자 경로 쿼리 검증 (필터 미적용 확인)
   - `PostMapper`/`CommentMapper` 익명화 로직 + `DEFAULT_PROFILE_URL` 상수
3. **`PostLike` 메타데이터 + `File` 메타데이터 + Repository 메서드** (PR3)
   - `PostLike.deletedAt`, `markDeleted`, `restore` 추가
   - **`PostLike` 조회 경로(좋아요 목록/존재 확인 등)에 `deletedAt IS NULL` 필터 추가**
   - `File`에 `deletedAt`/`hardDeleteAfter` 추가 (현재 스키마 확인 후 컬럼 결정)
   - `PostLikeRepository.findAllActiveByUserIdAndClubId`, `FileRepository.findAllByOwner` 추가
4. **`ClubActivityDeletionPolicy` + UseCase 확장 + API** (PR4)
   - Policy 클래스 신규
   - `ManageClubMemberUsecase.leave` 확장
   - `CannotLeaveAsLeadException` + `CANNOT_LEAVE_AS_LEAD` 에러 코드
   - Controller/UseCase 테스트, 통합 테스트
5. **`ktlintFormat` + 전체 테스트 실행** 후 머지

> **점진 머지 주의사항**
> - PR1~3은 단독 머지 가능하지만, 탈퇴 시 좋아요 차감/프로필 파일 정리는 PR4 머지 시점에 완성된다.
> - PR1 머지 후 ~ PR4 머지 전까지는 기존 동아리 탈퇴 흐름이 `member.leave(now)`만 호출하는 부분 적용 상태로 동작한다. 운영 영향은 없지만 QA 시점은 PR4 머지 이후로 잡는다.

### 작업 착수 시 사전 확인

| 항목 | 확인 내용 |
|---|---|
| `File` 엔티티 스키마 | `isDeleted` 컬럼 존재 여부 → 마이그레이션 SQL 확정 |
| `Post.decreaseLikeCount()` | 음수 보호 여부. 없으면 본 이슈에서 함께 보강 |
| `ClubMember` 권한 표현 필드 | `role` 인지 다른 이름인지 확인 후 `MemberRole.LEAD` 분기 코드 정정 |

## 확정 사항

| 항목 | 결정 |
|---|---|
| `Post`/`Comment` 처리 | 본문 보존. 매퍼에서 작성자만 익명화. 컬럼/메서드 변경 없음 |
| 댓글 본문 표시 | `"삭제된 댓글입니다."` 치환 안 함. 본문 보존 |
| 첨부 파일 (POST/COMMENT) | 본문 따라 보존. 30일 후 cascade hard delete |
| 출석/벌점 | 컬럼 추가 없음. 일반 경로는 `member.status = ACTIVE` 필터, 관리자 경로는 LEFT 포함 |
| `PostLike` | `post.likeCount` 즉시 차감 + `deletedAt` 마커로 30일 보관. 복구 시 `restore()` + `increaseLikeCount()`로 되돌림 |
| `LastNoticeRead`, `ClubMemberCardinal` | 컬럼 추가 없음. 30일 후 cascade hard delete |
| `File` (`CLUB_MEMBER_PROFILE`) | soft delete |
| 회계 데이터 | 본 이슈 범위 외. 동아리 삭제 이슈에서 처리 |
| 익명화 라벨 | `"탈퇴한 사용자"`, 프로필 이미지는 기본 이미지 |
| 관리자 경로 게시글 작성자 표시 | 기본 익명화 유지. 별도 정책 필요 시 후속 결정 |

## 브랜치 / 커밋 컨벤션

- 브랜치: `feat/WTH-390-club-leave`
- 커밋 예시:
  - `feat: Add leftAt and hardDeleteAfter to ClubMember`
  - `feat: Filter ACTIVE members in attendance and penalty queries`
  - `feat: Anonymize left member as author in post and comment mappers`
  - `feat: Add deletedAt marker to PostLike with restore support`
  - `feat: Add ClubActivityDeletionPolicy for member exit`
  - `feat: Extend club leave to soft delete member activities`
  - `test: Add ManageClubMemberUsecase leave scenarios`
