package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.Receipt
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptRepository : JpaRepository<Receipt, Long> {
    fun findAllByAccountIdOrderByCreatedAtDesc(accountId: Long): List<Receipt>
}
