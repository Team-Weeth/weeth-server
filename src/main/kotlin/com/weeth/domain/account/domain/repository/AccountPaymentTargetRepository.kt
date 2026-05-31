package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AccountPaymentTargetRepository : JpaRepository<AccountPaymentTarget, Long> {
    fun countByAccountIdAndTargetStatus(
        accountId: Long,
        targetStatus: AccountTargetStatus,
    ): Long

    fun countByAccountIdAndTargetStatusAndPaymentStatus(
        accountId: Long,
        targetStatus: AccountTargetStatus,
        paymentStatus: AccountPaymentStatus,
    ): Long
}
