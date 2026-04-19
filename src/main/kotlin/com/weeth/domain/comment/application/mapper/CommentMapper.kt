package com.weeth.domain.comment.application.mapper

import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import org.springframework.stereotype.Component

@Component
class CommentMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toCommentDto(
        comment: Comment,
        children: List<CommentResponse>,
        fileUrls: List<FileResponse>,
    ): CommentResponse =
        CommentResponse(
            id = comment.id,
            author =
                UserInfo.of(
                    comment.clubMember.user,
                    comment.clubMember.memberRole,
                    comment.clubMember.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
                ),
            content = comment.content,
            time = comment.createdAt,
            fileUrls = fileUrls,
            children = children,
        )
}
