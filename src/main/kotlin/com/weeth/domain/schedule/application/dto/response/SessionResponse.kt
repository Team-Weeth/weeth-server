package com.weeth.domain.schedule.application.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.weeth.domain.schedule.domain.entity.enums.Type
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SessionResponse(
    @field:Schema(description = "정기모임 ID", example = "1")
    val id: Long,
    @field:Schema(description = "제목", example = "1차 정기모임")
    val title: String,
    @field:Schema(description = "내용")
    val content: String?,
    @field:Schema(description = "장소", example = "공학관 401호")
    val location: String?,
    @field:Schema(description = "작성자 이름", example = "이지훈")
    val name: String?,
    @field:Schema(description = "기수", example = "8")
    val cardinal: Int,
    @field:Schema(description = "일정 타입", example = "MEETING")
    val type: Type,
    @field:Schema(description = "출석 코드", example = "1234")
    val code: Int?,
    @field:Schema(description = "시작 시간")
    val start: LocalDateTime,
    @field:Schema(description = "종료 시간")
    val end: LocalDateTime,
    @field:Schema(description = "생성 시간")
    val createdAt: LocalDateTime?,
    @field:Schema(description = "수정 시간")
    val modifiedAt: LocalDateTime?,
)
