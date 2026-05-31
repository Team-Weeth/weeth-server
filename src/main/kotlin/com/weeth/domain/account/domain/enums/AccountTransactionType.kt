package com.weeth.domain.account.domain.enums

enum class AccountTransactionType(
    val direction: AccountTransactionDirection,
) {
    DUES(AccountTransactionDirection.INCOME),
    CARRY_OVER(AccountTransactionDirection.INCOME),
    INCOME(AccountTransactionDirection.INCOME),
    EXPENSE(AccountTransactionDirection.EXPENSE),
}
