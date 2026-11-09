package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountPaymentTarget

interface AccountPaymentTargetReader {
    fun findByIdWithObligationDetails(id: Long): AccountPaymentTarget?
}
