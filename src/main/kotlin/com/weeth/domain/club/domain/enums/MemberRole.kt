package com.weeth.domain.club.domain.enums

enum class MemberRole {
    USER,
    ADMIN,
    LEAD, // 동아리 개설한 인원의 역할. 추후 LEAD 권한 이양 API도 추가
    ;

    fun isAdminOrLead(): Boolean = this == ADMIN || this == LEAD
}
