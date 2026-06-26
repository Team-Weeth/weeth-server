package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 멤버 탈퇴 시 함께 삭제 처리해야 하는 활동 정리 정책
 * 활동 정합성을 위해 타 도메인 Repository를 직접 의존
 * 탈퇴 시 정리 대상이 되는 활동을 한곳에서 관리
 *
 * TODO: 탈퇴/퇴출 시 DRAFT 상태 회비 장부의 AccountPaymentTarget(미납 TARGETED 행)도 여기서 정리해야 한다.
 *  현재는 회비 등록 완료(RegisterAccountUseCase.completeRegistration) 시점에 비활성 멤버의 미납 행을
 *  제외 처리하는 방식으로 보완 중이다. ACTIVE 장부의 행은 어드민 수동 환불 정책에 따라 유지한다.
 */
@Service
class ClubActivityDeletionPolicy(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val fileRepository: FileRepository,
) {
    fun markMemberActivitiesDeleted(
        member: ClubMember,
        now: LocalDateTime,
    ) {
        markMembersActivitiesDeleted(listOf(member), now)
    }

    fun markMembersActivitiesDeleted(
        members: List<ClubMember>,
        now: LocalDateTime,
    ) {
        if (members.isEmpty()) return

        markMembersFilesDeleted(members, now)
        markMembersPostLikesDeleted(members, now)
    }

    private fun markMembersFilesDeleted(
        members: List<ClubMember>,
        now: LocalDateTime,
    ) {
        val memberIds = members.map { it.id }.distinct().sorted()
        val postIds = postRepository.findActiveIdsByClubMemberIdIn(memberIds)
        val commentIds = commentRepository.findActiveIdsByClubMemberIdIn(memberIds)

        markFilesDeleted(FileOwnerType.POST, postIds, now)
        markFilesDeleted(FileOwnerType.COMMENT, commentIds, now)
    }

    private fun markFilesDeleted(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        now: LocalDateTime,
    ) {
        if (ownerIds.isEmpty()) return

        fileRepository.markActiveDeletedByOwnerTypeAndOwnerIdIn(
            ownerType = ownerType,
            ownerIds = ownerIds,
            deletedAt = now,
            hardDeleteAfter = File.retainedHardDeleteAfter(now),
        )
    }

    private fun markMembersPostLikesDeleted(
        members: List<ClubMember>,
        now: LocalDateTime,
    ) {
        members
            .groupBy { it.user.id }
            .forEach { (userId, userMembers) ->
                val clubIds = userMembers.map { it.club.id }.distinct().sorted()
                val postIds =
                    postLikeRepository
                        .findActivePostIdsByUserIdAndClubIdIn(
                            userId = userId,
                            clubIds = clubIds,
                        ).distinct()
                        .sorted()

                if (postIds.isEmpty()) return@forEach

                val postsById = postRepository.findAllByIdsWithLock(postIds).associateBy { it.id }
                if (postsById.isEmpty()) return@forEach

                val likes =
                    postLikeRepository.findAllActiveByUserIdAndPostIds(
                        userId = userId,
                        postIds = postsById.keys.toList(),
                    )

                for (like in likes) {
                    if (!like.markDeleted(now)) continue
                    postsById.getValue(like.post.id).decreaseLikeCount()
                }
            }
    }
}
