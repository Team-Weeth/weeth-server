package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.enums.PositionColor
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ClubPositionOptionTest :
    StringSpec({
        val club = ClubTestFixture.createClub()

        "정상적인 값으로 생성된다" {
            val option =
                ClubPositionOption.create(
                    club = club,
                    name = "백엔드",
                    color = PositionColor.PRIMARY,
                    displayOrder = 0,
                )

            option.name shouldBe "백엔드"
            option.color shouldBe PositionColor.PRIMARY
            option.displayOrder shouldBe 0
        }

        "이름이 10자를 초과하면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                ClubPositionOption.create(
                    club = club,
                    name = "12345678901",
                    color = PositionColor.PRIMARY,
                    displayOrder = 0,
                )
            }
        }

        "이름이 비어있으면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                ClubPositionOption.create(
                    club = club,
                    name = "",
                    color = PositionColor.PRIMARY,
                    displayOrder = 0,
                )
            }
        }

        "update로 이름/색상/순서를 변경할 수 있다" {
            val option =
                ClubPositionOption.create(
                    club = club,
                    name = "백엔드",
                    color = PositionColor.PRIMARY,
                    displayOrder = 0,
                )

            option.update(name = "프론트엔드", color = PositionColor.SECONDARY, displayOrder = 2)

            option.name shouldBe "프론트엔드"
            option.color shouldBe PositionColor.SECONDARY
            option.displayOrder shouldBe 2
        }

        "update 시 이름이 10자를 초과하면 예외가 발생한다" {
            val option =
                ClubPositionOption.create(
                    club = club,
                    name = "백엔드",
                    color = PositionColor.PRIMARY,
                    displayOrder = 0,
                )

            shouldThrow<IllegalArgumentException> {
                option.update(name = "12345678901", color = PositionColor.PRIMARY, displayOrder = 0)
            }
        }
    })
