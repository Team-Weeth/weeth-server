package com.weeth.domain.feedback.application.usecase.command

import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.feedback.application.dto.request.CreateFeedbackRequest
import com.weeth.domain.feedback.application.event.FeedbackCreatedEvent
import com.weeth.domain.feedback.application.exception.InvalidFeedbackContentException
import com.weeth.domain.feedback.domain.entity.Feedback
import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.feedback.domain.repository.FeedbackRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher

class CreateFeedbackUseCaseTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val feedbackRepository = mockk<FeedbackRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>()
        val useCase = CreateFeedbackUseCase(clubMemberPolicy, feedbackRepository, eventPublisher)

        val clubId = 1L
        val userId = 10L
        val request = CreateFeedbackRequest(content = "출석 체크 화면이 느려요.", channel = FeedbackChannel.CHAT)

        beforeTest {
            clearMocks(clubMemberPolicy, feedbackRepository, eventPublisher)
            every { feedbackRepository.save(any()) } answers { firstArg() }
            every { eventPublisher.publishEvent(any<Any>()) } returns Unit
        }

        describe("execute") {
            it("활성 멤버가 피드백을 저장하면 커밋 후 알림 이벤트를 발행한다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 10L)
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                val feedbackSlot = slot<Feedback>()
                val eventSlot = slot<FeedbackCreatedEvent>()
                every { feedbackRepository.save(capture(feedbackSlot)) } answers { firstArg() }
                every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

                useCase.execute(clubId, request, userId)

                feedbackSlot.captured.content shouldBe "출석 체크 화면이 느려요."
                feedbackSlot.captured.channel shouldBe FeedbackChannel.CHAT
                feedbackSlot.captured.clubId shouldBe clubId
                feedbackSlot.captured.user shouldBe member.user
                eventSlot.captured.clubName shouldBe member.club.name
                eventSlot.captured.authorName shouldBe member.user.name
                eventSlot.captured.channel shouldBe FeedbackChannel.CHAT
                eventSlot.captured.content shouldBe "출석 체크 화면이 느려요."
                verify(exactly = 1) { feedbackRepository.save(any()) }
                verify(exactly = 1) { eventPublisher.publishEvent(any<Any>()) }
            }

            it("활성 멤버가 아니면 저장과 이벤트 발행 없이 예외를 던진다") {
                every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                shouldThrow<MemberNotActiveException> {
                    useCase.execute(clubId, request, userId)
                }

                verify(exactly = 0) { feedbackRepository.save(any()) }
                verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
            }

            it("내용이 전각 공백뿐이면 저장과 이벤트 발행 없이 InvalidFeedbackContentException을 던진다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 10L)
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                val invalidRequest = CreateFeedbackRequest(content = "　　", channel = FeedbackChannel.CHAT)

                shouldThrow<InvalidFeedbackContentException> {
                    useCase.execute(clubId, invalidRequest, userId)
                }

                verify(exactly = 0) { feedbackRepository.save(any()) }
                verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
            }
        }
    })
