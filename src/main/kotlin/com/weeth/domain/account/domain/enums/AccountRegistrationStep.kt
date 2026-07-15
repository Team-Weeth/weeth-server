package com.weeth.domain.account.domain.enums

enum class AccountRegistrationStep(
    private val sequence: Int,
) {
    BASIC(10),
    PAYMENT_TARGETS(20),
    CARRY_OVER(30),
    BANK_ACCOUNT(40),
    REVIEW(50),
    ;

    fun isAtLeast(step: AccountRegistrationStep): Boolean = sequence >= step.sequence

    fun isAfter(step: AccountRegistrationStep): Boolean = sequence > step.sequence
}
