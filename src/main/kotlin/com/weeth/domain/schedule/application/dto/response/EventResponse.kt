package com.weeth.domain.schedule.application.dto.response

import com.weeth.domain.schedule.domain.entity.enums.Type
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class EventResponse(
    @field:Schema(description = "일정 ID", example = "1")
    val id: Long,
    @field:Schema(description = "일정 제목", example = "MT")
    val title: String,
    @field:Schema(description = "일정 내용")
    val content: String,
    @field:Schema(description = "장소", example = "가평")
    val location: String,
    @field:Schema(description = "작성자 이름", example = "이지훈")
    val name: String?,
    @field:Schema(description = "기수", example = "4")
    val cardinal: Int,
    @field:Schema(description = "일정 타입", example = "EVENT")
    val type: Type,
    @field:Schema(description = "시작 시간")
    val start: LocalDateTime,
    @field:Schema(description = "종료 시간")
    val end: LocalDateTime,
    @field:Schema(description = "생성 시간")
    val createdAt: LocalDateTime?,
    @field:Schema(description = "수정 시간")
    val modifiedAt: LocalDateTime?,
)
