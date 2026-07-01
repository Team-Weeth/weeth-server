package com.weeth.domain.account.application.dto.request

/**
 * 부원 거래 내역 목록 필터 탭.
 * 부원 개인 납부(`DUES`)는 어떤 탭에서도 개별 노출하지 않고, 항상 `duesSummary` 집계로만 제공한다.
 * - ALL: 수동 `INCOME` + 수동 `EXPENSE` + 이월 `CARRY_OVER` + 내 `REFUND`
 * - INCOME: 수동 수입(`INCOME`)만 (이월 `CARRY_OVER`/내 `REFUND` 제외)
 * - EXPENSE: 수동 지출(`EXPENSE`) + 내 `REFUND` (`source`는 `"환불"`로 마스킹)
 * - DUES: 개별 거래 없는 빈 목록 + `duesSummary` 집계만
 */
enum class AccountTransactionFilter {
    ALL,
    INCOME,
    EXPENSE,
    DUES,
}
