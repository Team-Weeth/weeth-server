package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Cardinal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cardinal_id")
    val id: Long = 0L,
    @Column(unique = true, nullable = false)
    val cardinalNumber: Int,
    var year: Int? = null,
    var semester: Int? = null,
    @Enumerated(EnumType.STRING)
    var status: CardinalStatus = CardinalStatus.DONE,
) : BaseEntity() {
    fun update(
        year: Int,
        semester: Int,
    ) {
        this.year = year
        this.semester = semester
    }

    fun inProgress() {
        status = CardinalStatus.IN_PROGRESS
    }

    fun done() {
        status = CardinalStatus.DONE
    }

    companion object {
        fun create(
            cardinalNumber: Int,
            year: Int? = null,
            semester: Int? = null,
            status: CardinalStatus = CardinalStatus.DONE,
        ): Cardinal =
            Cardinal(
                cardinalNumber = cardinalNumber,
                year = year,
                semester = semester,
                status = status,
            )
    }
}
