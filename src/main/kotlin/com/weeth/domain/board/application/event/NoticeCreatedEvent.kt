package com.weeth.domain.board.application.event

data class NoticeCreatedEvent(
    val clubId: Long,
    val boardId: Long,
    val postId: Long,
    val title: String,
    val authorUserId: Long,
)
