package com.weeth.domain.dashboard.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class DashboardUnreadNoticeResponse(
    @field:Schema(description = "게시글 ID", example = "1")
    val id: Long,
    @field:Schema(description = "게시판 ID", example = "1")
    val boardId: Long,
    @field:Schema(description = "공지 제목", example = "중간고사 기간 공지")
    val title: String,
    @field:Schema(description = "공지 내용", example = "이번 주 정기 모임은 중간고사 기간으로 인해 쉬어갑니다.")
    val content: String,
)
