package com.weeth.domain.cardinal.domain.entity

import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.club.domain.entity.Club
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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "cardinal",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_club_id_cardinal_number",
            columnNames = ["club_id", "cardinal_number"],
        ),
    ],
)
class Cardinal(
    club: Club,
    id: Long = 0L,
    @Column(nullable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club = club
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
            club: Club,
            cardinalNumber: Int,
            year: Int? = null,
            semester: Int? = null,
            status: CardinalStatus = CardinalStatus.DONE,
        ): Cardinal {
            require(cardinalNumber > 0) { "기수 번호는 0보다 커야 합니다." }
            year?.let { require(it > 0) { "연도는 0보다 커야 합니다." } }
            semester?.let { require(it in 1..2) { "학기는 1 또는 2여야 합니다." } }
            return Cardinal(
                club = club,
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
