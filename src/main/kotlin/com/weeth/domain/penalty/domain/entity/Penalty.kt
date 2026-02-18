package com.weeth.domain.penalty.domain.entity

import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cardinal_id")
    val cardinal: Cardinal,
    @Enumerated(EnumType.STRING)
    val penaltyType: PenaltyType,
    var penaltyDescription: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    val id: Long = 0,
) : BaseEntity() {
    fun update(penaltyDescription: String) {
        this.penaltyDescription = penaltyDescription
    }
}
