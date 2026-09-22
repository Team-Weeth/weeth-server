package com.weeth.domain.penalty.domain.entity

import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.fixture.PenaltyTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PenaltyTest :
    DescribeSpec({
        describe("create") {
            it("기본값으로 페널티를 생성한다") {
                val penalty = PenaltyTestFixture.createPenalty()

                penalty.penaltyType shouldBe PenaltyType.PENALTY
            }

            it("경고로 페널티를 생성한다") {
                val penalty = PenaltyTestFixture.createWarning()

                penalty.penaltyType shouldBe PenaltyType.WARNING
            }
        }

        describe("update") {
            it("페널티 설명을 수정한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")

                penalty.update(penaltyDescription = "새로운 사유")

                penalty.penaltyDescription shouldBe "새로운 사유"
            }

            it("null 값은 무시한다") {
                val penalty = PenaltyTestFixture.createPenalty(penaltyDescription = "원래 사유")

                penalty.update(penaltyDescription = null)

                penalty.penaltyDescription shouldBe "원래 사유"
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
