package com.weeth.domain.account.application.dto.request

/**
 * 거래 내역 목록 필터 탭.
 * - ALL: 전체
 * - INCOME: 수동 수입(`INCOME`)만 (납부 `DUES`/이월 `CARRY_OVER` 제외)
 * - EXPENSE: 지출 방향 전체(수동 `EXPENSE` + 시스템 `REFUND`)
 * - DUES: 회비 납부(`DUES`) + 이월(`CARRY_OVER`)
 */
enum class AccountTransactionFilter {
    ALL,
    INCOME,
    EXPENSE,
    DUES,
}
