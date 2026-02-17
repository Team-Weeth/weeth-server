package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GetAttendanceQueryService(
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val meetingGetService: MeetingGetService,
    private val attendanceRepository: AttendanceRepository,
    private val mapper: AttendanceMapper,
) {
    fun find(userId: Long): AttendanceMainResponse {
        val user = userGetService.find(userId)
        val today = LocalDate.now()

        val todayAttendance =
            user.attendances.firstOrNull { attendance ->
                attendance.meeting.start
                    .toLocalDate()
                    .isEqual(today) &&
                    attendance.meeting.end
                        .toLocalDate()
                        .isEqual(today)
            }

        return if (user.role == Role.ADMIN) {
            mapper.toAdminResponse(user, todayAttendance)
        } else {
            mapper.toMainResponse(user, todayAttendance)
        }
    }

    fun findAllDetailsByCurrentCardinal(userId: Long): AttendanceDetailResponse {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)

        val responses =
            user.attendances
                .filter { it.meeting.cardinal == currentCardinal.cardinalNumber }
                .sortedBy { it.meeting.start }
                .map(mapper::toResponse)

        return mapper.toDetailResponse(user, responses)
    }

    fun findAllAttendanceByMeeting(meetingId: Long): List<AttendanceInfoResponse> {
        val meeting = meetingGetService.find(meetingId)
        val attendances = attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)
        return attendances.map(mapper::toInfoResponse)
    }
}
