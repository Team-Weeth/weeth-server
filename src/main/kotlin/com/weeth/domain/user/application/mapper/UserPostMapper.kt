package com.weeth.domain.user.application.mapper

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserPostMapper {
    fun toMyPostResponse(
        post: Post,
        now: LocalDateTime,
    ): UserMyPostResponse =
        UserMyPostResponse(
            postId = post.id,
            clubId = TsidBase62Encoder.encode(post.board.club.id),
            clubName = post.board.club.name,
            boardId = post.board.id,
            boardName = post.board.name,
            title = post.title,
            content = post.content,
            commentCount = post.commentCount,
            likeCount = post.likeCount,
            createdAt = post.createdAt,
            isNew = post.createdAt.isAfter(now.minusHours(24)),
        )
}
