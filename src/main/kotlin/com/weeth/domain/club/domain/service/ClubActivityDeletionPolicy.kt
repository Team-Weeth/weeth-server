package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.club.domain.entity.ClubMember
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClubActivityDeletionPolicy(
    private val postLikeRepository: PostLikeRepository,
) {
    fun markMemberActivitiesDeleted(
        member: ClubMember,
        now: LocalDateTime,
    ) {
        postLikeRepository
            .findAllActiveByUserIdAndClubId(
                userId = member.user.id,
                clubId = member.club.id,
            ).forEach { like ->
                like.markDeleted(now)
                like.post.decreaseLikeCount()
            }
    }
}
