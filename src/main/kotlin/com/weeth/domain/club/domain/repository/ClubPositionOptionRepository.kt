package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubPositionOption
import org.springframework.data.jpa.repository.JpaRepository

interface ClubPositionOptionRepository :
    JpaRepository<ClubPositionOption, Long>,
    ClubPositionOptionReader {
    override fun findAllByClubIdOrderByDisplayOrderAsc(clubId: Long): List<ClubPositionOption>

    override fun findByIdOrNull(id: Long): ClubPositionOption? = findById(id).orElse(null)

    override fun findAllByIdIn(ids: List<Long>): List<ClubPositionOption>
}
