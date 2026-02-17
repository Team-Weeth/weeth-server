package com.weeth.domain.attendance.application.dto

import com.weeth.domain.attendance.domain.entity.enums.Status
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDateTime

class AttendanceDTO {
    data class Main(
        val attendanceRate: Int?,
        val title: String?,
        val status: Status?,
        @field:Schema(description = "어드민인 경우 출석 코드 노출")
        val code: Int?,
        val start: LocalDateTime?,
        val end: LocalDateTime?,
        val location: String?,
    )

    data class Detail(
        val attendanceCount: Int,
        val total: Int,
        val absenceCount: Int,
        val attendances: List<Response>,
    )

    data class Response(
        val id: Long,
        val status: Status?,
        val title: String?,
        val start: LocalDateTime?,
        val end: LocalDateTime?,
        val location: String?,
    )

    data class CheckIn(
        val code: Int,
    )

    data class AttendanceInfo(
        val id: Long,
        val status: Status?,
        val name: String?,
        val position: String?,
        val department: String?,
        val studentId: String?,
    )

    data class UpdateStatus(
        @field:NotNull
        val attendanceId: Long,
        @field:NotNull
        @field:Pattern(regexp = "ATTEND|ABSENT")
        val status: String,
    )
}
