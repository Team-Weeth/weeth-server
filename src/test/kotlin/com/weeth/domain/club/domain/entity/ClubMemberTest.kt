package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ClubMemberTest :
    StringSpec({
        val club =
            Club.create(
                name = "리츠",
                code = "LEETS001",
                schoolName = "가천대학교",
                clubContact =
                    ClubContact.from(
                        email = "leets@test.com",
                        phoneNumber = "010-0000-0000",
                        primaryContact = PrimaryContact.PHONE,
                    ),
            )
        val user = UserTestFixture.createActiveUser1()

        "ClubMember 생성 — 기본 상태는 WAITING, 역할은 USER, 패널티 횟수는 0" {
            val member = ClubMember(club = club, user = user)

            member.memberStatus shouldBe MemberStatus.WAITING
            member.memberRole shouldBe MemberRole.USER
            member.penaltyCount shouldBe 0
        }

        "accept — 상태를 ACTIVE로 전환한다" {
            val member = ClubMember(club = club, user = user)

            member.accept()

            member.memberStatus shouldBe MemberStatus.ACTIVE
        }

        "ban — 상태를 BANNED로 전환한다" {
            val member = ClubMember(club = club, user = user)

            member.ban()

            member.memberStatus shouldBe MemberStatus.BANNED
        }

        "leave — ACTIVE 상태에서 LEFT로 전환한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()

            member.leave()

            member.memberStatus shouldBe MemberStatus.LEFT
        }

        "isActive — ACTIVE 상태일 때 true" {
            val member = ClubMember(club = club, user = user)
            member.accept()

            member.isActive() shouldBe true
        }

        "isActive — WAITING 상태일 때 false" {
            val member = ClubMember(club = club, user = user)

            member.isActive() shouldBe false
        }

        "updateRole — 역할을 ADMIN으로 변경한다" {
            val member = ClubMember(club = club, user = user)

            member.updateRole(MemberRole.ADMIN)

            member.memberRole shouldBe MemberRole.ADMIN
            member.isAdmin() shouldBe true
        }

        "isAdmin — USER 역할일 때 false" {
            val member = ClubMember(club = club, user = user)

            member.isAdmin() shouldBe false
        }

        "attend/absent — 출석 통계를 올바르게 계산한다" {
            val member = ClubMember(club = club, user = user)
            member.attend()
            member.attend()
            member.absent()

            member.attendanceStats.attendanceCount shouldBe 2
            member.attendanceStats.absenceCount shouldBe 1
            member.attendanceStats.attendanceRate shouldBe (2 * 100 / 3)
        }

        "removeAttend — 출석 카운트를 감소시킨다" {
            val member = ClubMember(club = club, user = user)
            member.attend()
            member.attend()

            member.removeAttend()

            member.attendanceStats.attendanceCount shouldBe 1
        }

        "removeAbsent — 결석 카운트를 감소시킨다" {
            val member = ClubMember(club = club, user = user)
            member.absent()

            member.removeAbsent()

            member.attendanceStats.absenceCount shouldBe 0
        }

        "resetAttendanceStats — 출석 통계를 초기화한다" {
            val member = ClubMember(club = club, user = user)
            member.attend()
            member.attend()
            member.absent()

            member.resetAttendanceStats()

            member.attendanceStats.attendanceCount shouldBe 0
            member.attendanceStats.absenceCount shouldBe 0
            member.attendanceStats.attendanceRate shouldBe 0
        }

        "incrementPenaltyCount — 패널티를 증가시킨다" {
            val member = ClubMember(club = club, user = user)

            member.incrementPenaltyCount()
            member.incrementPenaltyCount()

            member.penaltyCount shouldBe 2
        }

        "decrementPenaltyCount — 패널티를 감소시킨다" {
            val member = ClubMember(club = club, user = user)
            member.incrementPenaltyCount()

            member.decrementPenaltyCount()

            member.penaltyCount shouldBe 0
        }

        "decrementPenaltyCount — 0일 때 감소해도 0을 유지한다" {
            val member = ClubMember(club = club, user = user)

            member.decrementPenaltyCount()

            member.penaltyCount shouldBe 0
        }

        "accept — WAITING이 아닌 상태에서 호출 시 예외가 발생한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()

            shouldThrow<IllegalStateException> {
                member.accept()
            }
        }

        "ban — 이미 BANNED 상태에서 호출 시 예외가 발생한다" {
            val member = ClubMember(club = club, user = user)
            member.ban()

            shouldThrow<IllegalStateException> {
                member.ban()
            }
        }

        "ban — LEFT 상태에서 호출 시 예외가 발생한다" {
            val member = ClubMember(club = club, user = user)
            member.accept()
            member.leave()

            shouldThrow<IllegalStateException> {
                member.ban()
            }
        }

        "leave — ACTIVE가 아닌 상태에서 호출 시 예외가 발생한다" {
            val member = ClubMember(club = club, user = user)

            shouldThrow<IllegalStateException> {
                member.leave()
            }
        }
    })
