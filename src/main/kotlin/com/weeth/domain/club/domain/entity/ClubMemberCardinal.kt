package com.weeth.domain.club.domain.entity

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "club_member_cardinal",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_club_member_id_cardinal_id",
            columnNames = ["club_member_id", "cardinal_id"],
        ),
    ],
)
class ClubMemberCardinal(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id", nullable = false)
    val clubMember: ClubMember,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cardinal_id", nullable = false)
    val cardinal: Cardinal,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    companion object {
        fun create(
            clubMember: ClubMember,
            cardinal: Cardinal,
        ): ClubMemberCardinal = ClubMemberCardinal(clubMember = clubMember, cardinal = cardinal)
    }
}
