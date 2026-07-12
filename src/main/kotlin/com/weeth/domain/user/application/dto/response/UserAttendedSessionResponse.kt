package com.weeth.domain.user.application.dto.response

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserAttendedSessionResponse(
    @field:Schema(description = "출석 ID", example = "1")
    val attendanceId: Long,
    @field:Schema(description = "동아리 ID", example = "1A2b3C")
    val clubId: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val clubName: String,
    @field:Schema(description = "세션 ID", example = "10")
    val sessionId: Long,
    @field:Schema(description = "세션 제목", example = "1차 정기모임")
    val sessionTitle: String,
    @field:Schema(description = "기수", example = "6")
    val cardinal: Int,
    @field:Schema(description = "시작 시각")
    val start: LocalDateTime,
    @field:Schema(description = "종료 시각")
    val end: LocalDateTime,
    @field:Schema(description = "출석 상태", example = "ATTEND")
    val status: AttendanceStatus,
)
