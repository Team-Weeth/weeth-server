package com.weeth.domain.feedback.domain.entity

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FeedbackTest :
    StringSpec({
        val user = UserTestFixture.createActiveUser1(1L)

        "create는 내용의 앞뒤 공백을 제거하고 피드백을 생성한다" {
            val feedback =
                Feedback.create(
                    user = user,
                    clubId = 10L,
                    channel = FeedbackChannel.CHAT,
                    content = "  내용입니다.  ",
                )

            feedback.user shouldBe user
            feedback.clubId shouldBe 10L
            feedback.channel shouldBe FeedbackChannel.CHAT
            feedback.content shouldBe "내용입니다."
        }

        "create는 내용이 공백뿐이면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> {
                Feedback.create(
                    user = user,
                    clubId = 10L,
                    channel = FeedbackChannel.CHAT,
                    content = "   ",
                )
            }
        }

        "create는 내용이 1000자를 초과하면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> {
                Feedback.create(
                    user = user,
                    clubId = 10L,
                    channel = FeedbackChannel.FORM,
                    content = "가".repeat(Feedback.MAX_CONTENT_LENGTH + 1),
                )
            }
        }

        "create는 내용이 정확히 1000자이면 통과한다" {
            val feedback =
                Feedback.create(
                    user = user,
                    clubId = 10L,
                    channel = FeedbackChannel.FORM,
                    content = "가".repeat(Feedback.MAX_CONTENT_LENGTH),
                )

            feedback.content.length shouldBe Feedback.MAX_CONTENT_LENGTH
        }
    })
