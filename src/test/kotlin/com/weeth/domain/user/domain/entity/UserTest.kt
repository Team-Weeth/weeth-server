package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import com.weeth.global.common.vo.PhoneNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class UserTest :
    StringSpec({
        "accept/ban/leave 상태 전환" {
            val user = User(name = "test", email = Email.from("test@test.com"), studentId = "20200001")

            user.accept()
            user.status shouldBe Status.ACTIVE

            user.ban()
            user.status shouldBe Status.BANNED

            user.leave(LocalDateTime.of(2026, 6, 12, 12, 0))
            user.status shouldBe Status.LEFT
        }

        "leave(now)는 탈퇴 상태와 삭제 예정일을 기록한다" {
            val user = User.create(name = "test", email = "test@test.com", status = Status.ACTIVE)
            val now = LocalDateTime.of(2026, 6, 12, 12, 0)

            user.leave(now)

            user.status shouldBe Status.LEFT
            user.leftAt shouldBe now
            user.hardDeleteAfter shouldBe now.plusDays(30)
        }

        "이미 LEFT 상태인 사용자가 leave(now)를 호출하면 예외가 발생한다" {
            val user = User.create(name = "test", email = "test@test.com", status = Status.ACTIVE)
            val now = LocalDateTime.of(2026, 6, 12, 12, 0)
            user.leave(now)

            shouldThrow<IllegalStateException> {
                user.leave(now.plusDays(1))
            }
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

        "missingProfileFields — 기본 생성 시 비어있는 필드 목록 반환" {
            val user = User.create(name = "test", email = "test@test.com")

            user.missingProfileFields() shouldContainExactlyInAnyOrder
                listOf("studentId", "tel", "school", "department")
        }

        "missingProfileFields — 모든 필드 채워졌을 때 빈 리스트 반환" {
            val user =
                User.create(
                    name = "test",
                    email = "test@test.com",
                    studentId = "20200001",
                    tel = "01012345678",
                    school = "가천대학교",
                    department = "CS",
                )

            user.missingProfileFields().shouldBeEmpty()
        }

        "missingProfileFields — 일부 필드만 비어있을 때 해당 필드만 반환" {
            val user =
                User.create(
                    name = "test",
                    email = "test@test.com",
                    studentId = "20200001",
                    tel = "01012345678",
                )

            user.missingProfileFields() shouldContainExactlyInAnyOrder listOf("school", "department")
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
            user.leave(LocalDateTime.of(2026, 6, 12, 12, 0))
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
