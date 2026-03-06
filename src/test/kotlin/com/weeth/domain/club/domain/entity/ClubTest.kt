package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.club.fixture.ClubTestFixture
import io.hypersistence.tsid.TSID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ClubTest :
    StringSpec({
        val defaultContact = ClubContact.from(email = "leets@test.com", phoneNumber = null)

        "Club 생성 — 이름과 코드를 가진다" {
            val club = Club.create(name = "리츠", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)

            club.name shouldBe "리츠"
            club.code shouldBe "LEETS001"
            club.description shouldBe null
        }

        "Club 생성 — 소개(description)를 선택적으로 가진다" {
            val club =
                Club.create(
                    name = "리츠",
                    code = "LEETS001",
                    description = "IT 동아리",
                    schoolName = "가천대학교",
                    clubContact = defaultContact,
                )

            club.description shouldBe "IT 동아리"
        }

        "update — 이름과 소개를 수정한다" {
            val club = Club.create(name = "리츠", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)

            club.update(name = "리츠2기", description = "업데이트된 소개")

            club.name shouldBe "리츠2기"
            club.description shouldBe "업데이트된 소개"
        }

        "update — 빈 이름은 예외가 발생한다" {
            val club = Club.create(name = "리츠", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)

            shouldThrow<IllegalArgumentException> {
                club.update(name = "", description = null)
            }
        }

        "update — 공백만 있는 이름은 예외가 발생한다" {
            val club = Club.create(name = "리츠", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)

            shouldThrow<IllegalArgumentException> {
                club.update(name = "   ", description = null)
            }
        }

        "regenerateCode — 초대 코드를 갱신한다" {
            val club = Club.create(name = "리츠", code = "OLD_CODE", schoolName = "가천대학교", clubContact = defaultContact)

            club.regenerateCode("NEW_CODE")

            club.code shouldBe "NEW_CODE"
        }

        "regenerateCode — 빈 코드는 예외가 발생한다" {
            val club = Club.create(name = "리츠", code = "OLD_CODE", schoolName = "가천대학교", clubContact = defaultContact)

            shouldThrow<IllegalArgumentException> {
                club.regenerateCode("")
            }
        }

        "create — Club id는 TSID 형식으로 생성된다" {
            val club = Club.create(name = "리츠", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)

            shouldNotThrowAny {
                TSID.from(club.id)
            }
        }

        "create - Club id는 TSID 형식으로 시간순 정렬이 가능하다" {
            val club1 = ClubTestFixture.createClub()
            val club2 = ClubTestFixture.createClub()

            club2.id shouldBeGreaterThan club1.id
        }

        "create — 유효한 인자로 생성에 성공한다" {
            val club =
                Club.create(
                    name = "리츠",
                    code = "LEETS001",
                    schoolName = "가천대학교",
                    clubContact = defaultContact,
                    description = "IT 동아리",
                )

            club.name shouldBe "리츠"
            club.code shouldBe "LEETS001"
            club.schoolName shouldBe "가천대학교"
            club.description shouldBe "IT 동아리"
        }

        "create — 빈 이름은 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                Club.create(name = "", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)
            }
        }

        "create — 공백만 있는 이름은 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                Club.create(name = "   ", code = "LEETS001", schoolName = "가천대학교", clubContact = defaultContact)
            }
        }

        "create — 빈 코드는 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                Club.create(name = "리츠", code = "", schoolName = "가천대학교", clubContact = defaultContact)
            }
        }

        "create — 빈 학교 이름은 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                Club.create(name = "리츠", code = "LEETS001", schoolName = "", clubContact = defaultContact)
            }
        }
    })
