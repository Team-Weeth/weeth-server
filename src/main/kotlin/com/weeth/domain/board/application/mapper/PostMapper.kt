package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.application.dto.response.BoardConfigResponse
import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostLikeActionResponse
import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
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
    fun toSaveResponse(post: Post) = PostSaveResponse(id = post.id, boardId = post.board.id)

    fun toLikeResponse(
        post: Post,
        isLiked: Boolean,
    ) = PostLikeResponse(isLiked = isLiked, likeCount = post.likeCount)

    fun toLikeActionResponse(
        post: Post,
        isLiked: Boolean,
    ) = PostLikeActionResponse(boardId = post.board.id, isLiked = isLiked, likeCount = post.likeCount)

    fun toDetailResponse(
        post: Post,
        comments: List<CommentResponse>,
        files: List<FileResponse>,
        isLiked: Boolean,
        now: LocalDateTime,
        memberRole: MemberRole,
    ) = PostDetailResponse(
        id = post.id,
        boardId = post.board.id,
        boardName = post.board.name,
        author = toAuthorInfo(post.clubMember),
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        like = toLikeResponse(post, isLiked),
        comments = comments,
        fileUrls = files,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
        boardConfig = BoardConfigResponse.of(post.board, memberRole),
    )

    fun toListResponse(
        post: Post,
        files: List<FileResponse>,
        now: LocalDateTime,
        isLiked: Boolean,
        memberRole: MemberRole,
    ) = PostListResponse(
        id = post.id,
        author = toAuthorInfo(post.clubMember),
        boardId = post.board.id,
        boardName = post.board.name,
        title = post.title,
        content = post.content,
        time = post.modifiedAt,
        commentCount = post.commentCount,
        like = toLikeResponse(post, isLiked),
        fileUrls = files,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
        boardConfig = BoardConfigResponse.of(post.board, memberRole),
    )

    private fun toAuthorInfo(member: ClubMember): UserInfo =
        UserInfo.ofClubMemberProfile(
            clubMember = member,
            profileName = member.userProfile?.name ?: member.user.name,
            resolvedProfileImageUrl = resolveProfileImage(member),
        )

    private fun resolveProfileImage(member: ClubMember): String? {
        val storageKey = member.userProfile?.profileImageStorageKey ?: member.profileImageStorageKey
        return storageKey?.let { fileAccessUrlPort.resolve(it) }
    }
}
