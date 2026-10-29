package com.weeth.domain.feedback.application.usecase.command

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.feedback.application.dto.request.CreateFeedbackRequest
import com.weeth.domain.feedback.application.event.FeedbackCreatedEvent
import com.weeth.domain.feedback.domain.entity.Feedback
import com.weeth.domain.feedback.domain.repository.FeedbackRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateFeedbackUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val feedbackRepository: FeedbackRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun execute(
        clubId: Long,
        request: CreateFeedbackRequest,
        userId: Long,
    ) {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        // @Valid(@NotNull/@NotBlank)로 컨트롤러 진입 전 검증되므로 이 지점에서는 항상 non-null이다.
        val channel = requireNotNull(request.channel) { "피드백 채널은 필수입니다." }
        val content = requireNotNull(request.content) { "피드백 내용은 필수입니다." }

        val feedback =
            Feedback.create(
                user = member.user,
                clubId = clubId,
                channel = channel,
                content = content,
            )
        feedbackRepository.save(feedback)

        eventPublisher.publishEvent(
            FeedbackCreatedEvent(
                clubName = member.club.name,
                authorName = member.user.name,
                channel = feedback.channel,
                content = feedback.content,
            ),
        )
    }
}
