package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionType
import org.springframework.data.jpa.repository.JpaRepository

interface AccountTransactionRepository : JpaRepository<AccountTransaction, Long> {
    fun countByAccountIdAndTypeInAndDeletedAtIsNull(
        accountId: Long,
        types: Collection<AccountTransactionType>,
    ): Long
}
