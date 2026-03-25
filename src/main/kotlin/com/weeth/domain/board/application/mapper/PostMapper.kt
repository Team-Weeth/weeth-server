package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PostMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toSaveResponse(post: Post) = PostSaveResponse(id = post.id)

    fun toLikeResponse(
        post: Post,
        isLiked: Boolean,
    ) = PostLikeResponse(isLiked = isLiked, likeCount = post.likeCount)

    fun toDetailResponse(
        post: Post,
        authorMember: ClubMember,
        comments: List<CommentResponse>,
        files: List<FileResponse>,
        isLiked: Boolean,
    ) = PostDetailResponse(
        id = post.id,
        boardId = post.board.id,
        boardName = post.board.name,
        author = UserInfo.of(post.user, authorMember.memberRole, resolveProfileImage(authorMember)),
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        like = toLikeResponse(post, isLiked),
        comments = comments,
        fileUrls = files,
    )

    fun toListResponse(
        post: Post,
        authorMember: ClubMember,
        hasFile: Boolean,
        now: LocalDateTime,
        isLiked: Boolean,
    ) = PostListResponse(
        id = post.id,
        author = UserInfo.of(post.user, authorMember.memberRole, resolveProfileImage(authorMember)),
        boardId = post.board.id,
        boardName = post.board.name,
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        like = toLikeResponse(post, isLiked),
        hasFile = hasFile,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
    )

    private fun resolveProfileImage(member: ClubMember): String? =
        member.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) }
}
