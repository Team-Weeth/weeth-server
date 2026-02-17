package com.weeth.domain.attendance.application.usecase

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.attendance.domain.service.AttendanceGetService
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AttendanceUseCaseImpl(
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val attendanceGetService: AttendanceGetService,
    private val attendanceUpdateService: AttendanceUpdateService,
    private val mapper: AttendanceMapper,
    private val meetingGetService: MeetingGetService,
) : AttendanceUseCase {
    @Transactional
    override fun checkIn(
        userId: Long,
        code: Int,
    ) {
        val user = userGetService.find(userId)
        val now = LocalDateTime.now()

        val todayAttendance =
            user.attendances.firstOrNull { attendance ->
                attendance.meeting.start
                    .minusMinutes(10)
                    .isBefore(now) &&
                    attendance.meeting.end.isAfter(now)
            } ?: throw AttendanceNotFoundException()

        if (todayAttendance.isWrong(code)) {
            throw AttendanceCodeMismatchException()
        }

        if (todayAttendance.status != Status.ATTEND) {
            attendanceUpdateService.attend(todayAttendance)
        }
    }

    override fun find(userId: Long): AttendanceMainResponse {
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

    override fun findAllDetailsByCurrentCardinal(userId: Long): AttendanceDetailResponse {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)

        val responses =
            user.attendances
                .filter { it.meeting.cardinal == currentCardinal.cardinalNumber }
                .sortedBy { it.meeting.start }
                .map(mapper::toResponse)

        return mapper.toDetailResponse(user, responses)
    }

    override fun findAllAttendanceByMeeting(meetingId: Long): List<AttendanceInfoResponse> {
        val meeting = meetingGetService.find(meetingId)
        val attendances = attendanceGetService.findAllByMeeting(meeting)
        return attendances.map(mapper::toInfoResponse)
    }

    // todo 차후 리팩토링 정기모임 id를 입력받아서 해당 정기모임의 출석을 마감하도록 수정
    override fun close(
        now: LocalDate,
        cardinal: Int,
    ) {
        val meetings = meetingGetService.find(cardinal)

        val targetMeeting =
            meetings.firstOrNull { meeting ->
                meeting.start.toLocalDate().isEqual(now) &&
                    meeting.end.toLocalDate().isEqual(now)
            } ?: throw MeetingNotFoundException()

        val attendanceList = attendanceGetService.findAllByMeeting(targetMeeting)
        attendanceUpdateService.close(attendanceList)
    }

    @Transactional
    override fun updateAttendanceStatus(attendanceUpdates: List<UpdateAttendanceStatusRequest>) {
        attendanceUpdates.forEach { update ->
            val attendance = attendanceGetService.findByAttendanceId(update.attendanceId)
            val user = attendance.user
            val newStatus = Status.valueOf(update.status)

            if (newStatus == Status.ABSENT) {
                attendance.close()
                user.removeAttend()
                user.absent()
            } else {
                attendance.attend()
                user.removeAbsent()
                user.attend()
            }
        }
    }
}
