package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostLikeLockTimeoutException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.PostLike
import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManagePostLikeUseCase(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val postMapper: PostMapper,
) {
    @Transactional
    fun like(
        clubId: Long,
        postId: Long,
        userId: Long,
    ): PostLikeResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val post =
            try {
                postRepository.findByIdWithLock(postId) ?: throw PostNotFoundException()
            } catch (_: PessimisticLockingFailureException) {
                throw PostLikeLockTimeoutException()
            }

        if (!post.belongsToClub(clubId)) throw PostNotFoundException()
        if (!post.board.isAccessibleBy(member.memberRole)) throw CategoryAccessDeniedException()

        val existingLike = postLikeRepository.findByPostAndUserId(post, userId)
        when {
            existingLike == null -> {
                postLikeRepository.save(PostLike(post = post, userId = userId))
                post.increaseLikeCount()
            }

            !existingLike.isActive -> {
                existingLike.activate()
                post.increaseLikeCount()
            }
        }

        return postMapper.toLikeResponse(post, isLiked = true)
    }

    @Transactional
    fun unlike(
        clubId: Long,
        postId: Long,
        userId: Long,
    ): PostLikeResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val post =
            try {
                postRepository.findByIdWithLock(postId) ?: throw PostNotFoundException()
            } catch (_: PessimisticLockingFailureException) {
                throw PostLikeLockTimeoutException()
            }

        if (!post.belongsToClub(clubId)) throw PostNotFoundException()
        if (!post.board.isAccessibleBy(member.memberRole)) throw CategoryAccessDeniedException()

        val existingLike = postLikeRepository.findByPostAndUserId(post, userId)
        if (existingLike != null && existingLike.isActive) {
            existingLike.deactivate()
            post.decreaseLikeCount()
        }

        return postMapper.toLikeResponse(post, isLiked = false)
    }
}
