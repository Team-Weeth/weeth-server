# WTH-232 기수별 출석 — FE 연동 계약

기수 선택에 따라 출석 기록과 통계를 함께 조회한다. BE 구현 및 FE 전달 문서이며,
FE 화면 변경과 운영 배포는 포함하지 않는다.

- 이슈: https://team-weeth.atlassian.net/browse/WTH-232
- FE 확인 기준: 로컬 `weeth-client`의 `92d9ae46`

## API

아래 경로의 공통 prefix는 `/api/v4`다. `clubId`는 Base62 TSID,
`cardinalNumber`는 기수 **번호**(예: 7)이며 DB 기수 ID가 아니다.

| API | 기수 지정 | 생략 |
|---|---|---|
| `GET /clubs/{clubId}/attendances?cardinalNumber=7` | 7기 출석률 + 7기의 오늘 출석 | 기존 누적 출석률 + 전체 기수의 오늘 출석 |
| `GET /clubs/{clubId}/attendances/detail?cardinalNumber=7` | 7기 기록·통계 | 본인 최신 소속 기수의 기록·통계 |
| `GET /admin/clubs/{clubId}/members?cardinalNumber=7&page=0&size=10` | 7기 멤버 목록 + 각 멤버의 7기 출석 통계 | 기존 멤버 목록 + 누적 통계 |
| `GET /admin/clubs/{clubId}/members/search?keyword=김&cardinalNumber=7` | 검색 결과의 7기 출석 통계 | 기존 누적 통계 |

사용자 요약/상세를 한 화면에서 표시할 때는 **동일한 기수 번호를 명시적으로 전송**한다.
기수 생략 의미가 다른 것은 기존 호출 호환성을 위한 것이다.
기수별 상세에 전체 기수 옵션은 이번 범위에 없다.
관리자 멤버 단건 상세·페널티 통계·마이페이지 활동 통계 및 참여 세션 목록은 기존 범위를 유지한다.

## 응답과 집계

기존 CommonResponse, 성공 코드(요약 10203 / 상세 10204), 기존 필드는 유지한다.
상세에 `cardinalNumber`, `attendanceRate`가 추가된다. 요약에는 `cardinalNumber`가
추가되며 기수 생략 시 null이다.

```json
{
  "code": 10204,
  "message": "사용자의 상세 출석 정보가 성공적으로 조회되었습니다.",
  "data": {
    "attendanceCount": 1,
    "absenceCount": 1,
    "total": 2,
    "cardinalNumber": 7,
    "attendanceRate": 50,
    "attendances": [
      { "id": 101, "status": "ATTEND", "title": "1주차", "start": "2026-03-07T10:00:00", "end": "2026-03-07T12:00:00", "location": "공학관" },
      { "id": 102, "status": "ABSENT", "title": "2주차", "start": "2026-03-14T10:00:00", "end": "2026-03-14T12:00:00", "location": "공학관" },
      { "id": 103, "status": "PENDING", "title": "3주차", "start": "2026-03-21T10:00:00", "end": "2026-03-21T12:00:00", "location": "공학관" }
    ]
  }
}
```

- `attendanceCount`: ATTEND, `absenceCount`: ABSENT.
- `total`: 출석 + 결석. **목록 길이가 아니다.** PENDING은 목록에 남고 total과 출석률 분모에서는 제외한다.
- `attendanceRate`: 출석 × 100 / total, 소수점 버림. total이 0이면 0.
- 해당 기수에 내 기록이 없으면 빈 목록과 0 통계. 다른 사람의 기록으로 대체하지 않는다.
- 기수 지정 요약에서 해당 기수의 오늘 세션이 없으면 세션 필드는 null, 출석률은 해당 기수 값이다.
- 관리자 기수 필터는 멤버별 누적 카운터를 변경하지 않고 페이지 멤버를 일괄 집계한다.
- 관리자 필터의 기록 없는 멤버는 0 통계이며, 페널티 횟수와 최근 페널티 일시는 기존 누적 의미를 유지한다.

## 오류 및 권한

- 사용자 API에서 동아리에 없는 기수: HTTP 404 / `21000 CARDINAL_NOT_FOUND`.
- 상세 기수 생략 시 본인의 소속 기수가 없음: 기존과 동일하게 HTTP 404 / 21000.
- 숫자가 아닌 기수: HTTP 400. null 문자열을 보내지 말고 생략한다.
- 동아리 활성 멤버/관리자 권한 검증은 유지된다. 기존 인증·권한 오류 처리도 유지한다.
- 관리자 목록/검색은 기존 필터 동작을 유지하므로 없는 기수에 대해 빈 결과를 반환한다.

## FE 적용 지점

### 사용자 출석 화면

1. `src/lib/apis/attendance.ts`, `attendance.server.ts`에 선택적 기수 파라미터를 추가한다.
2. `useAttendanceQuery.ts`의 인자, API 파라미터, queryKey에 모두 기수를 포함한다.
   예: `['attendance', clubId, 'summary', cardinalNumber ?? null]`.
3. `AttendanceContent.tsx`의 선택 기수와 `useCheckIn.ts`의 조회를 일치시킨다.
   `useCheckIn`은 현재 기수 없는 요약을 다시 조회하므로, 화면만 바꾸면 출석 상태/대상 세션이 어긋날 수 있다.
4. 기수 선택지는 기존 `GET /clubs/{clubId}/members/me`의 본인 `cardinals`를 활용할 수 있다.
   기본 선택은 본인 최대 기수 번호로 한다. 동아리 최신 기수와 본인 최신 기수를 혼동하지 않는다.
5. 상세의 `attendanceRate`를 그대로 표시하고, total 표기는 “확정 세션”처럼 미결 제외 의미가 드러나게 한다.

### 출석 상세 — 서버 조회에 주의

현재 `app/(private)/[clubId]/(main)/attendance/history/page.tsx`에서
`attendanceServerApi.getDetail(clubId)`로 읽고 `AttendanceHistoryContent`에 props로 전달한다.
클라이언트 드롭다운만 추가하면 재조회되지 않는다. 다음 중 하나로 구현한다.

- 서버 조회 유지: URL의 `?cardinalNumber=7`을 변경하고 page의 searchParams에서 읽어 서버 API로 전달한다.
- 클라이언트 조회 전환: `['attendance', clubId, 'detail', cardinalNumber]` 키로 조회하고 서버 초기값도 같은 기수에만 사용한다.

출석 메인 → 상세 이동 시 선택 기수를 URL로 전달한다. 서버와 클라이언트의 최초 기수가 일치해야 한다.

### 관리자 멤버 목록

`adminMember.ts`의 요청 타입에는 이미 `cardinalNumber`가 있다. 하지만
`useAdminMemberQueries.ts`의 일반/무한 스크롤 조회는 현재 page/size만 전송하고,
`useMemberListState.ts`에서 받은 페이지를 클라이언트 필터링한다.

- 선택 기수를 `useAdminMembers`와 `useAdminMembersInfinite`의 요청과 queryKey에 모두 추가한다.
- 일반 목록 예: `['admin', 'members', clubId, { page, size, cardinalNumber, keyword, sort }]`.
- 무한 목록도 기수별로 키를 분리하고, 변경 시 이전 기수 페이지/선택 멤버 상태를 초기화한다.
- 기수 필터는 서버에 맡긴다. 받은 한 페이지를 필터링하면 전체 건수와 페이지네이션이 틀어진다.
- `placeholderData`로 이전 기수 목록을 노출할 경우 새 기수 통계로 오인하지 않게 로딩 상태를 표시한다.

### 캐시 무효화

출석 성공, 관리자 출석 변경, 세션 종료/삭제, 멤버 기수 변경 후 영향받는 출석/멤버 캐시를 갱신한다.
기존 `useCheckIn`의 `['attendance', clubId]` prefix invalidate는 위 키 구조의 모든 기수 요약/상세에 적용된다.
관리자 변경에서는 `['admin', 'members', clubId]`도 무효화한다.
서버 컴포넌트 상세는 React Query invalidate 대상이 아니므로 필요 시 `router.refresh()` 또는 URL 이동으로 새로 읽는다.

## 연동 확인 시나리오

1. 7기 출석 1/결석 1/미결 1, 8기 출석 9인 멤버에서 7기 선택 → total 2, 50%, 기록 3개.
2. 7 ↔ 8 변경 시 각각 API 요청과 캐시가 구분된다. 요약/상세가 같은 기수다.
3. 기록 없는 기수 → 빈 목록/0%. 관리자 목록에서 누적값으로 대체되지 않는다.
4. 관리자 데스크톱 페이지와 모바일 무한 목록 모두 기수 선택 시 서버 재조회한다.
5. 출석 변경 뒤 해당 기수 통계와 목록이 함께 갱신된다.
6. 기존 기수 없는 클라이언트의 요약 호출이 유지되고, 상세 통계는 최신 기수 기록과 일치한다.

## 구현 검증

독립 implementation-evaluator 판정: **PASS** (2026-09-24, dev `71fc976` 기준 재이식본, 1회 재평가).

| 평가 기준 | 결과 및 근거 |
|---|---|
| 요구사항 충족 | 통과 — 선택 기수 조회, 생략 시 기본값, 미결 제외, 관리자 페이지 단일 GROUP BY 일괄 집계, 기록 없는 멤버 0 통계, 누적 카운터 불변 |
| 아키텍처 준수 | 통과 — ArchitectureTest 12개, 타 도메인 Reader 사용, 출석률 계산은 `ClubAttendanceStats` 재사용 |
| 테스트 | 통과 — `./gradlew test` 945개 실패 0 (기존 `@Disabled` 4개 제외 스킵 0), ktlintCheck 통과 |
| API 계약 | 통과 — CommonResponse와 기존 성공 코드 유지, 404/21000 및 400 검증, 신규 에러 코드 없음 |

FE 화면 E2E와 운영 배포는 이번 검증 범위에 포함하지 않았다.
