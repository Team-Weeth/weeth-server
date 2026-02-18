package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.Status
import io.swagger.v3.oas.annotations.media.Schema

data class AttendanceInfoResponse(
    @field:Schema(description = "출석 ID", example = "1")
    val id: Long,
    @field:Schema(description = "출석 상태", example = "ATTEND")
    val status: Status?,
    @field:Schema(description = "사용자 이름", example = "이지훈")
    val name: String?,
    @field:Schema(description = "직책", example = "BE")
    val position: String?,
    @field:Schema(description = "소속 학과", example = "컴퓨터공학과")
    val department: String?,
    @field:Schema(description = "학번", example = "20201234")
    val studentId: String?,
)
