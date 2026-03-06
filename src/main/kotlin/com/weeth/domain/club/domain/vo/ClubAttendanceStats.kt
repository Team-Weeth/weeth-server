package com.weeth.domain.club.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class ClubAttendanceStats(
    attendanceCount: Int = 0,
    absenceCount: Int = 0,
    attendanceRate: Int = 0,
) {
    @Column(name = "attendance_count")
    var attendanceCount: Int = attendanceCount
        private set

    @Column(name = "absence_count")
    var absenceCount: Int = absenceCount
        private set

    @Column(name = "attendance_rate")
    var attendanceRate: Int = attendanceRate
        private set

    fun reset() {
        attendanceCount = 0
        absenceCount = 0
        attendanceRate = 0
    }

    fun attend() {
        attendanceCount++
        recalculateRate()
    }

    fun removeAttend() {
        if (attendanceCount > 0) {
            attendanceCount--
            recalculateRate()
        }
    }

    fun absent() {
        absenceCount++
        recalculateRate()
    }

    fun removeAbsent() {
        if (absenceCount > 0) {
            absenceCount--
            recalculateRate()
        }
    }

    private fun recalculateRate() {
        val total = attendanceCount + absenceCount
        attendanceRate = if (total > 0) (attendanceCount * 100) / total else 0
    }
}
