package com.weeth.domain.session.domain.entity

import com.weeth.domain.session.domain.enums.RecurrenceType
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "session_group")
class SessionGroup(
    title: String,
    recurrenceType: RecurrenceType,
    recurrenceEndDate: LocalDate,
    cardinal: Int,
    startTime: LocalTime,
    endTime: LocalTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    var title: String = title
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var recurrenceType: RecurrenceType = recurrenceType
        private set

    var recurrenceEndDate: LocalDate = recurrenceEndDate
        private set

    var cardinal: Int = cardinal
        private set

    // 반복 기준 시작 시각
    var startTime: LocalTime = startTime
        private set

    // 반복 기준 종료 시각
    var endTime: LocalTime = endTime
        private set
}
