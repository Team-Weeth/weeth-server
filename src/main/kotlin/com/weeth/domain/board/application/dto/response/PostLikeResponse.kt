package com.weeth.domain.board.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PostLikeResponse(
    @field:Schema(description = "좋아요 여부", example = "true")
    val isLiked: Boolean,
    @field:Schema(description = "좋아요 수", example = "5")
    val likeCount: Int,
)
