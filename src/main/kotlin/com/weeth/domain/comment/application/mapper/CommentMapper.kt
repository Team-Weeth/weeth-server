package com.weeth.domain.comment.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
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
        authorMember: ClubMember,
        children: List<CommentResponse>,
        fileUrls: List<FileResponse>,
    ): CommentResponse =
        CommentResponse(
            id = comment.id,
            author =
                UserInfo.of(
                    comment.user,
                    authorMember.memberRole,
                    authorMember.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
                ),
            content = comment.content,
            time = comment.modifiedAt,
            fileUrls = fileUrls,
            children = children,
        )
}
