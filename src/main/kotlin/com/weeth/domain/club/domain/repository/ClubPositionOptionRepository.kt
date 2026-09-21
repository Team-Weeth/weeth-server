package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubPositionOption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubPositionOptionRepository :
    JpaRepository<ClubPositionOption, Long>,
    ClubPositionOptionReader {
    override fun findAllByClubIdOrderByDisplayOrderAsc(clubId: Long): List<ClubPositionOption>

    override fun findByIdOrNull(id: Long): ClubPositionOption? = findById(id).orElse(null)

    override fun findAllByIdIn(ids: List<Long>): List<ClubPositionOption>

    @Modifying(flushAutomatically = true)
    @Query(
        """
        DELETE FROM ClubPositionOption p
        WHERE p.club.id = :clubId
        """,
    )
    fun hardDeleteAllByClubId(
        @Param("clubId") clubId: Long,
    ): Int
}
