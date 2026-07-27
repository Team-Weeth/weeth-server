package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.domain.user.application.mapper.UserAttendanceMapper
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserAttendanceQueryService(
    private val attendanceReader: AttendanceReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val userAttendanceMapper: UserAttendanceMapper,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    @Transactional(readOnly = true)
    fun getAttendedSessions(
        userId: Long,
        clubId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): SliceResponse<UserAttendedSessionResponse> {
        validatePage(pageNumber, pageSize)
        clubMemberPolicy.getActiveMember(clubId, userId)
        val pageable = PageRequest.of(pageNumber, pageSize)
        val attendances =
            attendanceReader.findByUserIdAndClubIdAndStatus(
                userId,
                clubId,
                AttendanceStatus.ATTEND,
                pageable,
            )
        return SliceResponse.from(attendances.map(userAttendanceMapper::toAttendedSessionResponse))
    }

    private fun validatePage(
        pageNumber: Int,
        pageSize: Int,
    ) {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) {
            throw UserPageNotFoundException()
        }
    }
}
