package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UserTest :
    StringSpec({
        "accept/ban/leave 상태 전환" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")

            user.accept()
            user.status shouldBe Status.ACTIVE

            user.ban()
            user.status shouldBe Status.BANNED

            user.leave()
            user.status shouldBe Status.LEFT
        }

        "attendance 카운터 및 출석률 계산" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")
            user.attend()
            user.attend()
            user.absent()

            user.attendanceCount shouldBe 2
            user.absenceCount shouldBe 1
            user.attendanceRate shouldBe (2 * 100 / 3)
        }

        "updateRole / hasRole" {
            val user = User(name = "test", email = "test@test.com", studentId = "20200001")
            user.updateRole(Role.ADMIN)

            user.hasRole(Role.ADMIN) shouldBe true
        }
    })
