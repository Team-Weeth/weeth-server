package com.weeth.domain.penalty.domain.entity

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Penalty(
    clubMember: ClubMember,
    cardinal: Cardinal,
    penaltyType: PenaltyType,
    penaltyDescription: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    var id: Long = 0L
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    var clubMember: ClubMember = clubMember
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cardinal_id")
    var cardinal: Cardinal = cardinal
        private set

    @Enumerated(EnumType.STRING)
    var penaltyType: PenaltyType = PenaltyType.PENALTY
        private set

    var penaltyDescription: String = penaltyDescription
        private set

    fun update(penaltyDescription: String) {
        this.penaltyDescription = penaltyDescription
    }
}
