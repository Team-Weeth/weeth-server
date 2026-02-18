package com.weeth.domain.comment.application.mapper

import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.dto.response.FileResponse
import org.springframework.stereotype.Component

@Component
class CommentMapper {
    fun toCommentDto(
        comment: Comment,
        children: List<CommentResponse>,
        fileUrls: List<FileResponse>,
    ): CommentResponse =
        CommentResponse(
            id = comment.id,
            name = comment.user.name,
            position = comment.user.position,
            role = comment.user.role,
            content = comment.content,
            time = comment.modifiedAt,
            fileUrls = fileUrls,
            children = children,
        )
}
