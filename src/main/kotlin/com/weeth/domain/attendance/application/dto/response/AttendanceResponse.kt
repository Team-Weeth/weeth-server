package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.Status
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AttendanceResponse(
    @field:Schema(description = "출석 ID", example = "1")
    val id: Long,
    @field:Schema(description = "출석 상태", example = "ATTEND")
    val status: Status?,
    @field:Schema(description = "정기모임 제목", example = "1주차 정기모임")
    val title: String?,
    @field:Schema(description = "정기모임 시작 시간")
    val start: LocalDateTime?,
    @field:Schema(description = "정기모임 종료 시간")
    val end: LocalDateTime?,
    @field:Schema(description = "정기모임 장소", example = "공학관 401호")
    val location: String?,
)
