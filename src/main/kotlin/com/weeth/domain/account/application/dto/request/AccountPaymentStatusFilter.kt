package com.weeth.domain.account.application.dto.request

import com.weeth.domain.account.domain.enums.AccountPaymentStatus

/**
 * 부원별 납부현황 목록의 필터 탭(전체/완료/미납/환불/제외).
 * 요청 측 계약이므로 domain enums 가 아닌 request 패키지에 둔다.
 *
 * 전체/완료/미납/환불은 납부 대상(TARGETED)의 납부 상태 축이고, 제외(EXCLUDED)는 대상 상태 축이라
 * 조회 경로가 다르다 — [isExcluded] 로 분기한다.
 */
enum class AccountPaymentStatusFilter {
    ALL,
    PAID,
    UNPAID,
    REFUNDED,
    EXCLUDED,
    ;

    /** 제외 탭은 납부 상태가 아니라 대상 상태 축이라 명부 기반 조회 경로를 탄다. */
    val isExcluded: Boolean
        get() = this == EXCLUDED

    /** ALL/EXCLUDED 는 납부 상태 필터 없음(null), 나머지는 대응 도메인 상태로 변환한다. */
    fun toDomainOrNull(): AccountPaymentStatus? =
        when (this) {
            ALL, EXCLUDED -> null
            PAID -> AccountPaymentStatus.PAID
            UNPAID -> AccountPaymentStatus.UNPAID
            REFUNDED -> AccountPaymentStatus.REFUNDED
        }
}
