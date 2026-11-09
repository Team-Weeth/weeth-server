package com.weeth.domain.account.application.event

data class DuesObligationCreatedEvent(
    val clubId: Long,
    val accountId: Long,
    val paymentTargetId: Long,
    val userId: Long,
    val accountName: String,
    val amount: Int,
)
