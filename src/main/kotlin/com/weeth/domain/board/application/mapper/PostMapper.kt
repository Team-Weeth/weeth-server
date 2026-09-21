package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.application.dto.response.BoardConfigResponse
import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostLikeActionResponse
import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.application.mapper.UserInfoMapper
import com.weeth.global.common.markdown.MarkdownToTiptapHtmlConverter
import com.weeth.global.config.properties.LegacyContentProperties
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PostMapper(
    private val userInfoMapper: UserInfoMapper,
    private val markdownConverter: MarkdownToTiptapHtmlConverter,
    private val legacyContentProperties: LegacyContentProperties,
) {
    /**
     * v3 에디터로 작성된 본문만 Tiptap HTML로 변환해 내려준다.
     * 설정이 비어 있거나 v4에서 작성된 글이면 저장된 본문을 그대로 반환한다.
     */
    private fun renderContent(post: Post): String {
        val clubId = legacyContentProperties.clubId ?: return post.content
        val editorMigratedAt = legacyContentProperties.editorMigratedAt ?: return post.content

        return if (post.hasLegacyMarkdownContent(clubId, editorMigratedAt)) {
            markdownConverter.convert(post.content)
        } else {
            post.content
        }
    }

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
        author = userInfoMapper.toClubMemberAuthorInfo(post.clubMember),
        title = post.title,
        content = renderContent(post),
        time = post.createdAt,
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
        author = userInfoMapper.toClubMemberAuthorInfo(post.clubMember),
        boardId = post.board.id,
        boardName = post.board.name,
        title = post.title,
        content = renderContent(post),
        time = post.createdAt,
        commentCount = post.commentCount,
        like = toLikeResponse(post, isLiked),
        fileUrls = files,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
        boardConfig = BoardConfigResponse.of(post.board, memberRole),
    )
}
