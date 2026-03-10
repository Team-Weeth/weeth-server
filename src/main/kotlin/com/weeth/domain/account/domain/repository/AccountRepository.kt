package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByCardinal(cardinal: Int): Account?

    fun existsByCardinal(cardinal: Int): Boolean

    @Query("SELECT a FROM Account a WHERE a.club.id = :clubId AND a.cardinal = :cardinal")
    fun findByClubIdAndCardinal(
        @Param("clubId") clubId: Long,
        @Param("cardinal") cardinal: Int,
    ): Account?

    @Query(
        "SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE a.club.id = :clubId AND a.cardinal = :cardinal",
    )
    fun existsByClubIdAndCardinal(
        @Param("clubId") clubId: Long,
        @Param("cardinal") cardinal: Int,
    ): Boolean
}
