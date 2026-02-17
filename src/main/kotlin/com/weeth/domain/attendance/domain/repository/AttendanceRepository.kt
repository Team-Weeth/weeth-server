package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findAllByMeetingAndUserStatus(
        meeting: Meeting,
        status: Status,
    ): List<Attendance>

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.meeting = :meeting")
    fun deleteAllByMeeting(meeting: Meeting)
}
