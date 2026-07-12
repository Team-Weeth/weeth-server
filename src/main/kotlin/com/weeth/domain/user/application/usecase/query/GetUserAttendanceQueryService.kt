package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.domain.user.application.mapper.UserAttendanceMapper
import com.weeth.global.common.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserAttendanceQueryService(
    private val attendanceReader: AttendanceReader,
    private val userAttendanceMapper: UserAttendanceMapper,
) {
    @Transactional(readOnly = true)
    fun getAttendedSessions(
        userId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): PageResponse<UserAttendedSessionResponse> {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val attendances = attendanceReader.findByUserIdAndStatus(userId, AttendanceStatus.ATTEND, pageable)
        return PageResponse.from(attendances.map(userAttendanceMapper::toAttendedSessionResponse))
    }
}
