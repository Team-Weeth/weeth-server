package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PostMapper {
    fun toSaveResponse(post: Post) = PostSaveResponse(id = post.id)

    fun toDetailResponse(
        post: Post,
        comments: List<CommentResponse>,
        files: List<FileResponse>,
    ) = PostDetailResponse(
        id = post.id,
        name = post.user.name,
        role = post.user.role,
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        comments = comments,
        fileUrls = files,
    )

    fun toListResponse(
        post: Post,
        hasFile: Boolean,
        now: LocalDateTime,
    ) = PostListResponse(
        id = post.id,
        name = post.user.name,
        role = post.user.role,
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        hasFile = hasFile,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
    )
}
