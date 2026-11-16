package com.weeth.domain.user.domain.enums

/**
 * 동아리와 무관한 플랫폼 전역 권한. 동아리 단위 권한([com.weeth.domain.club.domain.enums.MemberRole])과 구분된다.
 * 값이 없으면(`null`) 일반 사용자다. 현재는 개발자 전용 권한만 존재한다(화면 부여 기능은 1차 범위 아님).
 */
enum class PlatformRole {
    /** 릴리즈 노트 작성/수정/삭제/발행 등 플랫폼 운영 권한 */
    DEVELOPER,
}
