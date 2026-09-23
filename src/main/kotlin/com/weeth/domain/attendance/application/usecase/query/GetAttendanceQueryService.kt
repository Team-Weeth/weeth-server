package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse
import com.weeth.domain.attendance.application.dto.response.AttendanceSummaryResponse
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GetAttendanceQueryService(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMemberCardinalPolicy: ClubMemberCardinalPolicy,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val attendanceMapper: AttendanceMapper,
    private val cardinalReader: CardinalReader,
) {
    fun findAttendance(
        clubId: Long,
        userId: Long,
        cardinalNumber: Int? = null,
    ): AttendanceSummaryResponse {
        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)
        val stats =
            if (cardinalNumber == null) {
                clubMember.attendanceStats
            } else {
                cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinalNumber)
                    ?: throw CardinalNotFoundException()
                attendanceMapper.toStats(
                    attendanceRepository.findAllByClubMemberIdAndCardinal(clubMember.id, cardinalNumber),
                )
            }
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayAttendances =
            attendanceRepository
                .findTodayByClubMemberId(
                    clubMember.id,
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay(),
                ).filter { cardinalNumber == null || it.session.cardinal == cardinalNumber }

        val todayAttendance =
            when {
                todayAttendances.size <= 1 -> {
                    todayAttendances.firstOrNull()
                }

                else -> {
                    todayAttendances.firstOrNull { it.session.start >= now }
                        ?: todayAttendances.last()
                }
            }

        return attendanceMapper.toSummaryResponse(clubMember, todayAttendance, stats, cardinalNumber)
    }

    fun findAllDetailsByCurrentCardinal(
        clubId: Long,
        userId: Long,
        cardinalNumber: Int? = null,
    ): AttendanceDetailResponse {
        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)
        val selectedCardinal =
            if (cardinalNumber == null) {
                clubMemberCardinalPolicy.getCurrentCardinal(clubMember)
            } else {
                cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinalNumber)
                    ?: throw CardinalNotFoundException()
            }
        val responses =
            attendanceRepository
                .findAllByClubMemberIdAndCardinal(clubMember.id, selectedCardinal.cardinalNumber)

        return attendanceMapper.toDetailResponse(selectedCardinal.cardinalNumber, responses)
    }

    fun findAllAttendanceBySession(
        clubId: Long,
        userId: Long,
        sessionId: Long,
    ): List<AttendanceInfoResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val session = sessionReader.getById(sessionId)

        if (session.club.id != clubId) {
            throw AttendanceNotFoundException()
        }

        val attendances = attendanceRepository.findAllBySession(session)
        return attendances.map(attendanceMapper::toInfoResponse)
    }
}
