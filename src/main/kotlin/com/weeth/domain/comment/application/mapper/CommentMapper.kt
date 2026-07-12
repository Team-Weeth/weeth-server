package com.weeth.domain.comment.application.mapper

import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.application.mapper.UserInfoMapper
import org.springframework.stereotype.Component

@Component
class CommentMapper(
    private val userInfoMapper: UserInfoMapper,
) {
    fun toCommentDto(
        comment: Comment,
        children: List<CommentResponse>,
        fileUrls: List<FileResponse>,
    ): CommentResponse =
        CommentResponse(
            id = comment.id,
            author = userInfoMapper.toClubMemberAuthorInfo(comment.clubMember),
            content = comment.content,
            time = comment.createdAt,
            fileUrls = fileUrls,
            children = children,
        )
}
