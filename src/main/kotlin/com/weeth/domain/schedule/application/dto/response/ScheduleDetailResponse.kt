package com.weeth.domain.schedule.application.dto.response

import com.weeth.domain.schedule.domain.enums.Type
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ScheduleDetailResponse(
    @field:Schema(description = "일정 ID", example = "1")
    val id: Long,
    @field:Schema(description = "일정 유형", example = "SESSION")
    val type: Type,
    @field:Schema(description = "제목", example = "1주차 정기모임")
    val title: String,
    @field:Schema(description = "설명")
    val description: String?,
    @field:Schema(description = "장소", example = "가천대 체육관")
    val location: String?,
    @field:Schema(description = "시작 시간")
    val start: LocalDateTime,
    @field:Schema(description = "종료 시간")
    val end: LocalDateTime,
    @field:Schema(description = "생성자 이름", example = "홍길동")
    val creatorName: String?,
    @field:Schema(description = "내 출석 상태 (SESSION만, EVENT는 null)")
    val myAttendanceStatus: ScheduleAttendanceStatus?,
    @field:Schema(description = "출석 완료 시간 (COMPLETED일 때만, 나머지는 null)")
    val attendedAt: LocalDateTime?,
    @field:Schema(description = "총 참석자 수 (SESSION만, EVENT는 null)")
    val totalAttendees: Int?,
    @field:Schema(description = "참석자 목록 (SESSION만, EVENT는 null)")
    val attendees: List<AttendeeResponse>?,
)
