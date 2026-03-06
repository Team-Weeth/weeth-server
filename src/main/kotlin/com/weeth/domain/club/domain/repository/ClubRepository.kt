package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.application.exception.ClubNotFoundException
import com.weeth.domain.club.domain.entity.Club
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ClubRepository :
    JpaRepository<Club, Long>,
    ClubReader {
    fun findByCode(code: String): Optional<Club>

    fun findByName(name: String): Optional<Club>

    override fun getClubById(clubId: Long): Club = findById(clubId).orElseThrow { ClubNotFoundException() }

    override fun findByIdOrNull(clubId: Long): Club? = findById(clubId).orElse(null)

    override fun findClubByCode(code: String): Club? = findByCode(code).orElse(null)

    override fun findClubByName(name: String): Club? = findByName(name).orElse(null)
}
