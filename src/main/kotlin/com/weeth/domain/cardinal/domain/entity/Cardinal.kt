package com.weeth.domain.cardinal.domain.entity

import com.weeth.domain.cardinal.domain.enums.CardinalStatus
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
    id: Long = 0L,
    @Column(unique = true, nullable = false)
    val cardinalNumber: Int,
    year: Int? = null,
    semester: Int? = null,
    status: CardinalStatus = CardinalStatus.DONE,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cardinal_id")
    var id: Long = id
        private set

    var year: Int? = year
        private set

    var semester: Int? = semester
        private set

    @Enumerated(EnumType.STRING)
    var status: CardinalStatus = status
        private set

    fun update(
        year: Int,
        semester: Int,
    ) {
        validatePeriod(year, semester)
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
        ): Cardinal {
            require(cardinalNumber > 0) { "기수 번호는 0보다 커야 합니다." }
            year?.let { require(it > 0) { "연도는 0보다 커야 합니다." } }
            semester?.let { require(it in 1..2) { "학기는 1 또는 2여야 합니다." } }
            return Cardinal(
                cardinalNumber = cardinalNumber,
                year = year,
                semester = semester,
                status = status,
            )
        }

        private fun validatePeriod(
            year: Int,
            semester: Int,
        ) {
            require(year > 0) { "연도는 0보다 커야 합니다." }
            require(semester in 1..2) { "학기는 1 또는 2여야 합니다." }
        }
    }
}
