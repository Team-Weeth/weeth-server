package com.weeth.domain.comment.application.mapper

import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.application.dto.response.UserInfo
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
            author = UserInfo.from(comment.user),
            content = comment.content,
            time = comment.modifiedAt,
            fileUrls = fileUrls,
            children = children,
        )
}
