package com.weeth.domain.attendance.application.usecase

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse
import java.time.LocalDate

interface AttendanceUseCase {
    fun checkIn(
        userId: Long,
        code: Int,
    )

    fun find(userId: Long): AttendanceMainResponse

    fun findAllDetailsByCurrentCardinal(userId: Long): AttendanceDetailResponse

    fun findAllAttendanceByMeeting(meetingId: Long): List<AttendanceInfoResponse>

    fun close(
        now: LocalDate,
        cardinal: Int,
    )

    fun updateAttendanceStatus(attendanceUpdates: List<UpdateAttendanceStatusRequest>)
}
