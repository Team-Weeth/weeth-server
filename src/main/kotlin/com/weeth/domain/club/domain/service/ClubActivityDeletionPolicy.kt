package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.entity.ClubMember
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClubActivityDeletionPolicy(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
) {
    fun markMemberActivitiesDeleted(
        member: ClubMember,
        now: LocalDateTime,
    ) {
        val postIds =
            postLikeRepository
                .findActivePostIdsByUserIdAndClubId(
                    userId = member.user.id,
                    clubId = member.club.id,
                ).distinct()
                .sorted()

        if (postIds.isEmpty()) return

        val postsById = postRepository.findAllByIdsWithLock(postIds).associateBy { it.id }
        if (postsById.isEmpty()) return

        postLikeRepository
            .findAllActiveByUserIdAndPostIds(
                userId = member.user.id,
                postIds = postsById.keys.toList(),
            ).forEach { like ->
                if (!like.markDeleted(now)) return@forEach
                postsById.getValue(like.post.id).decreaseLikeCount()
            }
    }
}
