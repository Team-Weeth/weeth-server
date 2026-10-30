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

        "create는 내용이 올바르지 않으면 IllegalArgumentException을 던진다" {
            shouldThrow<IllegalArgumentException> {
                Feedback.create(
                    user = user,
                    clubId = 10L,
                    channel = FeedbackChannel.CHAT,
                    content = "   ",
                )
            }
        }

        "isValidContent는 ASCII 공백뿐이면 false를 반환한다" {
            Feedback.isValidContent("   ") shouldBe false
        }

        "isValidContent는 전각 공백(U+3000)뿐이면 false를 반환한다" {
            // `@NotBlank`는 U+3000을 공백으로 인식하지 못해 DTO 검증을 통과하므로,
            // 엔티티 단계에서 Kotlin의 isBlank()로 최종 검증해야 한다.
            Feedback.isValidContent("　　") shouldBe false
        }

        "isValidContent는 줄바꿈 없는 공백(U+00A0)뿐이면 false를 반환한다" {
            Feedback.isValidContent("  ") shouldBe false
        }

        "isValidContent는 내용이 비어있지 않고 1000자 이하면 true를 반환한다" {
            Feedback.isValidContent("가".repeat(Feedback.MAX_CONTENT_LENGTH)) shouldBe true
        }

        "isValidContent는 내용이 1000자를 초과하면 false를 반환한다" {
            Feedback.isValidContent("가".repeat(Feedback.MAX_CONTENT_LENGTH + 1)) shouldBe false
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
