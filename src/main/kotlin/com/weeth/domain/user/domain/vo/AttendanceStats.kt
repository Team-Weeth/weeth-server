package com.weeth.domain.user.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class AttendanceStats(
    @Column(name = "attendance_count")
    var attendanceCount: Int = 0,
    @Column(name = "absence_count")
    var absenceCount: Int = 0,
    @Column(name = "attendance_rate")
    var attendanceRate: Int = 0,
) {
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
