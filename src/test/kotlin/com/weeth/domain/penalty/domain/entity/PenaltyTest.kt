package com.weeth.domain.penalty.domain.entity

import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.fixture.PenaltyTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PenaltyTest :
    DescribeSpec({
        describe("create") {
            it("기본값으로 페널티를 생성한다") {
                val penalty = PenaltyTestFixture.createPenalty()

                penalty.penaltyType shouldBe PenaltyType.PENALTY
                penalty.score shouldBe 1
            }

            it("경고로 페널티를 생성한다") {
                val penalty = PenaltyTestFixture.createWarning()

                penalty.penaltyType shouldBe PenaltyType.WARNING
                penalty.score shouldBe 1
            }

            it("커스텀 점수로 페널티를 생성한다") {
                val penalty = PenaltyTestFixture.createPenalty(score = 5)

                penalty.score shouldBe 5
            }
        }

        describe("update") {
            it("페널티 설명을 수정한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")

                penalty.update(penaltyDescription = "새로운 사유")

                penalty.penaltyDescription shouldBe "새로운 사유"
            }

            it("페널티 점수를 수정한다") {
                val penalty = PenaltyTestFixture.createPenalty(score = 1)

                penalty.update(score = 5)

                penalty.score shouldBe 5
            }

            it("페널티 설명과 점수를 함께 수정한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유", score = 1)

                penalty.update(penaltyDescription = "새로운 사유", score = 3)

                penalty.penaltyDescription shouldBe "새로운 사유"
                penalty.score shouldBe 3
            }

            it("null 값은 무시한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유", score = 2)

                penalty.update(penaltyDescription = null, score = null)

                penalty.penaltyDescription shouldBe "원래 사유"
                penalty.score shouldBe 2
            }

            it("점수가 0 이하면 예외를 던진다") {
                val penalty = PenaltyTestFixture.createPenalty()

                shouldThrow<IllegalArgumentException> {
                    penalty.update(score = 0)
                }
            }

            it("음수 점수는 예외를 던진다") {
                val penalty = PenaltyTestFixture.createPenalty()

                shouldThrow<IllegalArgumentException> {
                    penalty.update(score = -1)
                }
            }

            it("공백만 있는 설명도 업데이트한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")

                penalty.update(penaltyDescription = "   ")

                penalty.penaltyDescription shouldBe "   "
            }

            it("빈 문자열 설명도 업데이트한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")

                penalty.update(penaltyDescription = "")

                penalty.penaltyDescription shouldBe ""
            }
        }
    })
