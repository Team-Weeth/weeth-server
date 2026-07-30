package com.weeth.domain.club.application.dto.request

/**
 * 어드민 멤버 목록 정렬 옵션.
 *
 * 기수는 [com.weeth.domain.club.domain.entity.ClubMemberCardinal] 별도 엔티티(멤버 1:N 기수)라
 * Spring `Sort` 프로퍼티로 매핑할 수 없다. 그래서 다른 정렬 enum과 달리 `toSort()`를 두지 않고,
 * 리포지토리 쿼리의 `ORDER BY`에서 최신(최대) 기수번호 스칼라 서브쿼리와 함께 처리한다.
 * 어떤 옵션이든 마지막 타이브레이커는 `clubMemberId ASC`라 페이지 경계에서 중복/누락이 없다.
 */
enum class ClubMemberSort {
    /** 최신 기수 우선. 기수가 없는 멤버는 뒤로 밀린다. */
    CARDINAL_DESC,

    /** 오래된 기수 우선. 기수가 없는 멤버가 먼저 온다. */
    CARDINAL_ASC,

    /** 이름 가나다순 */
    NAME_ASC,

    /** 최근 가입순 */
    JOINED_DESC,
    ;

    /** 리포지토리 `ORDER BY`가 분기 기준으로 사용하는 키. */
    val queryKey: String
        get() = name
}
