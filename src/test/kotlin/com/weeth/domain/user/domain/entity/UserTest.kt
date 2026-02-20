package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.entity.enums.Role
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UserTest :
    StringSpec({
        "accept/ban/leave 상태 전환" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")

            user.accept()
            user.isInactive() shouldBe false

            user.ban()
            user.isInactive() shouldBe true

            user.leave()
            user.isInactive() shouldBe true
        }

        "attendance 카운터 및 출석률 계산" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")
            user.attend()
            user.attend()
            user.absent()

            user.attendanceCount shouldBe 2
            user.absenceCount shouldBe 1
            user.attendanceRate shouldBe 66
        }

        "updateRole / hasRole" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")
            user.updateRole(Role.ADMIN)

            user.hasRole(Role.ADMIN) shouldBe true
        }
    })
