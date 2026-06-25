package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.comment.domain.repository.CommentRepository
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
        markMemberFilesDeleted(member, now)
        markMemberPostLikesDeleted(member, now)
    }

    private fun markMemberFilesDeleted(
        member: ClubMember,
        now: LocalDateTime,
    ) {
        val postIds = postRepository.findActiveIdsByClubMemberIdAndClubId(member.id, member.club.id)
        val commentIds = commentRepository.findActiveIdsByClubMemberIdAndClubId(member.id, member.club.id)

        markFilesDeleted(FileOwnerType.POST, postIds, now)
        markFilesDeleted(FileOwnerType.COMMENT, commentIds, now)
    }

    private fun markFilesDeleted(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        now: LocalDateTime,
    ) {
        if (ownerIds.isEmpty()) return

        fileRepository
            .findAllActiveByOwnerTypeAndOwnerIdIn(ownerType, ownerIds)
            .forEach { it.markDeleted(now) }
    }

    private fun markMemberPostLikesDeleted(
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
