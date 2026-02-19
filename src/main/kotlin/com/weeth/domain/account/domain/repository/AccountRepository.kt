package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByCardinal(cardinal: Int): Account?

    fun existsByCardinal(cardinal: Int): Boolean
}
