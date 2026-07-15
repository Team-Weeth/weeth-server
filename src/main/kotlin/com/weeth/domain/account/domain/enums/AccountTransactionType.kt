package com.weeth.domain.account.domain.enums

enum class AccountTransactionType(
    val direction: AccountTransactionDirection,
) {
    DUES(AccountTransactionDirection.INCOME),
    CARRY_OVER(AccountTransactionDirection.INCOME),
    INCOME(AccountTransactionDirection.INCOME),
    EXPENSE(AccountTransactionDirection.EXPENSE),

    // 환불: 납부현황 환불 액션에서만 시스템 생성. 직접 등록·수정·삭제 불가.
    REFUND(AccountTransactionDirection.EXPENSE),
}
