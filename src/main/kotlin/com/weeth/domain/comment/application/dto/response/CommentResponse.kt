package com.weeth.domain.comment.application.dto.response

import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val name: String,
    val position: Position,
    val role: Role,
    val content: String,
    val time: LocalDateTime,
    val fileUrls: List<FileResponse>,
    val children: List<CommentResponse>,
)
