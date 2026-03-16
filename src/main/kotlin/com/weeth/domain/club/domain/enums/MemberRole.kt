package com.weeth.domain.club.domain.enums

enum class MemberRole {
    USER,
    ADMIN,
    LEAD, // 동아리 개설한 인원의 역할. 추후 LEAD 권한 이양 API도 추가
    // TODO: ADMIN, LEAD 권한 관련 JWT, Filter
    // 다른 동아리의 ADMIN인 경우는 JWT로 검증이 안되니까 JWT에서 Role을 빼야할 수도 있음
}
