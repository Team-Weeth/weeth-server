package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.application.exception.ClubNotFoundException
import com.weeth.domain.club.domain.entity.Club
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.util.Optional

interface ClubRepository :
    JpaRepository<Club, Long>,
    ClubReader {
    fun findByCode(code: String): Optional<Club>

    fun findByName(name: String): Optional<Club>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT c FROM Club c WHERE c.id = :clubId")
    fun findByIdWithLock(clubId: Long): Club?

    fun existsBySchoolNameAndName(
        schoolName: String,
        name: String,
    ): Boolean

    override fun getClubById(clubId: Long): Club = findById(clubId).orElseThrow { ClubNotFoundException() }

    override fun getClubByIdForUpdate(clubId: Long): Club = findByIdWithLock(clubId) ?: throw ClubNotFoundException()

    override fun findByIdOrNull(clubId: Long): Club? = findById(clubId).orElse(null)

    override fun findClubByCode(code: String): Club? = findByCode(code).orElse(null)

    override fun findClubByName(name: String): Club? = findByName(name).orElse(null)
}
