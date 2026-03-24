package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostLikeLockTimeoutException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.domain.entity.PostLike
import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TogglePostLikeUseCase(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun execute(
        clubId: Long,
        postId: Long,
        userId: Long,
    ): PostLikeResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)

        val post =
            try {
                postRepository.findByIdWithLock(postId) ?: throw PostNotFoundException()
            } catch (e: PessimisticLockingFailureException) {
                throw PostLikeLockTimeoutException()
            }

        if (post.board.club.id != clubId || post.board.isDeleted) throw PostNotFoundException()
        if (!post.board.isAccessibleBy(member.memberRole)) throw CategoryAccessDeniedException()

        val existingLike = postLikeRepository.findByPostAndUserId(post, userId)

        return if (existingLike != null) {
            existingLike.toggle()
            if (existingLike.isActive) post.increaseLikeCount() else post.decreaseLikeCount()
            PostLikeResponse(isLiked = existingLike.isActive, likeCount = post.likeCount)
        } else {
            postLikeRepository.save(PostLike(post = post, userId = userId))
            post.increaseLikeCount()
            PostLikeResponse(isLiked = true, likeCount = post.likeCount)
        }
    }
}
