package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import com.weeth.domain.user.domain.vo.PhoneNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UserTest :
    StringSpec({
        "accept/ban/leave 상태 전환" {
            val user = User(name = "test", email = Email.from("test@test.com"), studentId = "20200001")

            user.accept()
            user.status shouldBe Status.ACTIVE

            user.ban()
            user.status shouldBe Status.BANNED

            user.leave()
            user.status shouldBe Status.LEFT
        }

        "User.create 기본 status는 WAITING이다" {
            val user = User.create(name = "test", email = "test@test.com")

            user.status shouldBe Status.WAITING
        }

        "User.create에 status를 명시하면 해당 상태로 생성된다" {
            val user = User.create(name = "test", email = "test@test.com", status = Status.ACTIVE)

            user.status shouldBe Status.ACTIVE
        }

        "생성 시 빈 이름은 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                User(name = "   ", email = Email.from("test@test.com"))
            }
        }

        "update에서 빈 이름은 예외가 발생한다" {
            val user = User(name = "test", email = Email.from("test@test.com"))

            shouldThrow<IllegalArgumentException> {
                user.update(
                    name = "",
                    email = Email.from("test@test.com"),
                    studentId = "123",
                    tel = PhoneNumber.from("01012345678"),
                    school = "가천대학교",
                    department = "CS",
                )
            }
        }

        "프로필 미완성 판정 — 기본 생성 시 false" {
            val user = User.create(name = "test", email = "test@test.com")

            user.isProfileCompleted() shouldBe false
        }

        "프로필 완성 판정 — 모든 필드 채워졌을 때 true" {
            val user =
                User.create(
                    name = "test",
                    email = "test@test.com",
                    studentId = "20200001",
                    tel = "01012345678",
                    school = "가천대학교",
                    department = "CS",
                )

            user.isProfileCompleted() shouldBe true
        }

        "isActive / isInactive 동작" {
            val user = User(name = "test", email = Email.from("test@test.com"))
            user.isActive() shouldBe false
            user.isInactive() shouldBe true

            user.accept()
            user.isActive() shouldBe true
            user.isInactive() shouldBe false
        }

        "isBannedOrLeft 동작" {
            val user = User(name = "test", email = Email.from("test@test.com"))
            user.isBannedOrLeft() shouldBe false

            user.ban()
            user.isBannedOrLeft() shouldBe true

            user.accept()
            user.leave()
            user.isBannedOrLeft() shouldBe true
        }

        "agreeTerms 성공 — 모두 true" {
            val user = User(name = "test", email = Email.from("test@test.com"))

            user.agreeTerms(termsAgreed = true, privacyAgreed = true)

            user.termsAgreed shouldBe true
            user.privacyAgreed shouldBe true
        }

        "agreeTerms 실패 — termsAgreed가 false" {
            val user = User(name = "test", email = Email.from("test@test.com"))

            shouldThrow<IllegalArgumentException> {
                user.agreeTerms(termsAgreed = false, privacyAgreed = true)
            }
        }

        "agreeTerms 실패 — privacyAgreed가 false" {
            val user = User(name = "test", email = Email.from("test@test.com"))

            shouldThrow<IllegalArgumentException> {
                user.agreeTerms(termsAgreed = true, privacyAgreed = false)
            }
        }
    })
