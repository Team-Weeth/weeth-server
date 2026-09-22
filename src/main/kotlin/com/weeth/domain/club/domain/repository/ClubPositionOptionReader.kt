package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubPositionOption

interface ClubPositionOptionReader {
    fun findAllByClubIdOrderByDisplayOrderAsc(clubId: Long): List<ClubPositionOption>

    fun findByIdOrNull(id: Long): ClubPositionOption?

    fun findAllByIdIn(ids: List<Long>): List<ClubPositionOption>
}
