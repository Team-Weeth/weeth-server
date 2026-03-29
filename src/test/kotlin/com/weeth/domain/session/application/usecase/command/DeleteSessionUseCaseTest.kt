package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.application.exception.ClosedSessionIncludedException
import com.weeth.domain.session.application.exception.SessionGroupNotFoundException
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.enums.UpdateScope
import com.weeth.domain.session.domain.repository.SessionGroupRepository
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class DeleteSessionUseCaseTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val sessionGroupRepository = mockk<SessionGroupRepository>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)

        val useCase =
            DeleteSessionUseCase(sessionRepository, attendanceRepository, sessionGroupRepository, clubPermissionPolicy)

        val clubId = 1L
        val userId = 10L
        val club = ClubTestFixture.createClub(id = clubId)

        beforeTest {
            clearMocks(sessionRepository, attendanceRepository, sessionGroupRepository, clubPermissionPolicy)
            every { attendanceRepository.findAllBySessionAndClubMemberMemberStatusWithLock(any(), any()) } returns
                emptyList()
            every { attendanceRepository.findAllBySessionInAndClubMemberMemberStatusWithLock(any(), any()) } returns
                emptyList()
            every { attendanceRepository.deleteAllBySession(any()) } just Runs
            every { attendanceRepository.deleteAllBySessionIn(any()) } just Runs
            every { sessionRepository.delete(any()) } just Runs
            every { sessionRepository.deleteAll(any<List<Session>>()) } just Runs
            every { sessionGroupRepository.delete(any()) } just Runs
        }

        describe("delete") {
            context("존재하지 않는 세션") {
                it("예외를 던진다") {
                    every { sessionRepository.findByIdWithLock(99L) } returns null

                    shouldThrow<SessionNotFoundException> {
                        useCase.delete(clubId, 99L, userId)
                    }
                }
            }

            context("다른 클럽의 세션") {
                it("예외를 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 999L)
                    val session = SessionTestFixture.createSession(id = 1L, club = otherClub)
                    every { sessionRepository.findByIdWithLock(1L) } returns session

                    shouldThrow<SessionNotFoundException> {
                        useCase.delete(clubId, 1L, userId)
                    }
                }
            }

            context("단일 세션 삭제") {
                it("세션과 출석 레코드를 삭제한다") {
                    val session = SessionTestFixture.createSession(id = 1L, club = club)
                    every { sessionRepository.findByIdWithLock(1L) } returns session

                    useCase.delete(clubId, 1L, userId)

                    verify(exactly = 1) { attendanceRepository.deleteAllBySession(session) }
                    verify(exactly = 1) { sessionRepository.delete(session) }
                }
            }

            context("반복 세션 THIS_ONLY 삭제") {
                it("해당 세션만 삭제하고 그룹은 유지한다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session = SessionTestFixture.createSession(id = 1L, club = club, sessionGroup = group)
                    every { sessionRepository.findByIdWithLock(1L) } returns session
                    every { sessionGroupRepository.findByIdWithLock(1L) } returns group
                    every { sessionRepository.countBySessionGroup(group) } returns 3L

                    useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_ONLY)

                    verify(exactly = 1) { sessionRepository.delete(session) }
                    verify(exactly = 0) { sessionGroupRepository.delete(any()) }
                }

                it("마지막 세션 삭제 시 그룹도 함께 삭제한다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session = SessionTestFixture.createSession(id = 1L, club = club, sessionGroup = group)
                    every { sessionRepository.findByIdWithLock(1L) } returns session
                    every { sessionGroupRepository.findByIdWithLock(1L) } returns group
                    every { sessionRepository.countBySessionGroup(group) } returns 0L

                    useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_ONLY)

                    verify(exactly = 1) { sessionGroupRepository.delete(group) }
                }
            }

            context("반복 세션 THIS_AND_FUTURE 삭제") {
                it("해당 세션부터 이후 모든 세션을 삭제한다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session1 =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )
                    val session2 =
                        SessionTestFixture.createSession(
                            id = 2L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 8, 10, 0),
                            end = LocalDateTime.of(2026, 4, 8, 12, 0),
                        )
                    val futureSessions = listOf(session1, session2)

                    every { sessionRepository.findByIdWithLock(1L) } returns session1
                    every {
                        sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(group, session1.start)
                    } returns futureSessions
                    every { sessionGroupRepository.findByIdWithLock(1L) } returns group
                    every { sessionRepository.countBySessionGroup(group) } returns 2L

                    useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_AND_FUTURE)

                    verify(exactly = 1) { attendanceRepository.deleteAllBySessionIn(futureSessions) }
                    verify(exactly = 1) { sessionRepository.deleteAll(futureSessions) }
                }

                it("CLOSED 세션 포함 시 force=false이면 예외를 던진다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val openSession =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )
                    val closedSession =
                        SessionTestFixture.createSession(
                            id = 2L,
                            club = club,
                            sessionGroup = group,
                            status = SessionStatus.CLOSED,
                            start = LocalDateTime.of(2026, 4, 8, 10, 0),
                            end = LocalDateTime.of(2026, 4, 8, 12, 0),
                        )

                    every { sessionRepository.findByIdWithLock(1L) } returns openSession
                    every {
                        sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(
                            group,
                            openSession.start,
                        )
                    } returns listOf(openSession, closedSession)

                    shouldThrow<ClosedSessionIncludedException> {
                        useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_AND_FUTURE, force = false)
                    }
                }

                it("CLOSED 세션 포함 시 force=true이면 정상 삭제된다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val openSession =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )
                    val closedSession =
                        SessionTestFixture.createSession(
                            id = 2L,
                            club = club,
                            sessionGroup = group,
                            status = SessionStatus.CLOSED,
                            start = LocalDateTime.of(2026, 4, 8, 10, 0),
                            end = LocalDateTime.of(2026, 4, 8, 12, 0),
                        )

                    every { sessionRepository.findByIdWithLock(1L) } returns openSession
                    every {
                        sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(
                            group,
                            openSession.start,
                        )
                    } returns listOf(openSession, closedSession)
                    every { sessionGroupRepository.findByIdWithLock(1L) } returns group
                    every { sessionRepository.countBySessionGroup(group) } returns 2L

                    shouldNotThrowAny {
                        useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_AND_FUTURE, force = true)
                    }

                    verify(exactly = 1) { sessionRepository.deleteAll(listOf(openSession, closedSession)) }
                }

                it("모든 세션을 삭제하면 그룹도 함께 삭제된다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session1 =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )
                    val session2 =
                        SessionTestFixture.createSession(
                            id = 2L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 8, 10, 0),
                            end = LocalDateTime.of(2026, 4, 8, 12, 0),
                        )

                    every { sessionRepository.findByIdWithLock(1L) } returns session1
                    every {
                        sessionRepository.findAllBySessionGroupAndStartGreaterThanEqualWithLock(group, session1.start)
                    } returns listOf(session1, session2)
                    every { sessionGroupRepository.findByIdWithLock(1L) } returns group
                    every { sessionRepository.countBySessionGroup(group) } returns 0L

                    useCase.delete(clubId, 1L, userId, scope = UpdateScope.THIS_AND_FUTURE)

                    verify(exactly = 1) { sessionGroupRepository.delete(group) }
                }
            }
        }

        describe("deleteGroup") {
            context("존재하지 않는 그룹") {
                it("예외를 던진다") {
                    every { sessionGroupRepository.findById(99L) } returns Optional.empty()

                    shouldThrow<SessionGroupNotFoundException> {
                        useCase.deleteGroup(clubId, 99L, userId)
                    }
                }
            }

            context("다른 클럽의 세션 그룹") {
                it("예외를 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 999L)
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session = SessionTestFixture.createSession(id = 1L, club = otherClub, sessionGroup = group)

                    every { sessionGroupRepository.findById(1L) } returns Optional.of(group)
                    every { sessionRepository.findAllBySessionGroupWithLock(group) } returns listOf(session)

                    shouldThrow<SessionGroupNotFoundException> {
                        useCase.deleteGroup(clubId, 1L, userId)
                    }
                }
            }

            context("그룹 전체 삭제") {
                it("모든 세션과 출석을 삭제하고 그룹을 삭제한다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session1 =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )
                    val session2 =
                        SessionTestFixture.createSession(
                            id = 2L,
                            club = club,
                            sessionGroup = group,
                            start = LocalDateTime.of(2026, 4, 8, 10, 0),
                            end = LocalDateTime.of(2026, 4, 8, 12, 0),
                        )
                    val sessions = listOf(session1, session2)

                    every { sessionGroupRepository.findById(1L) } returns Optional.of(group)
                    every { sessionRepository.findAllBySessionGroupWithLock(group) } returns sessions

                    useCase.deleteGroup(clubId, 1L, userId)

                    verify(exactly = 1) { attendanceRepository.deleteAllBySessionIn(sessions) }
                    verify(exactly = 1) { sessionRepository.deleteAll(sessions) }
                    verify(exactly = 1) { sessionGroupRepository.delete(group) }
                }
            }

            context("CLOSED 세션 포함") {
                it("force=false이면 예외를 던진다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val closedSession =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            status = SessionStatus.CLOSED,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )

                    every { sessionGroupRepository.findById(1L) } returns Optional.of(group)
                    every { sessionRepository.findAllBySessionGroupWithLock(group) } returns listOf(closedSession)

                    shouldThrow<ClosedSessionIncludedException> {
                        useCase.deleteGroup(clubId, 1L, userId, force = false)
                    }
                }

                it("force=true이면 정상 삭제된다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val closedSession =
                        SessionTestFixture.createSession(
                            id = 1L,
                            club = club,
                            sessionGroup = group,
                            status = SessionStatus.CLOSED,
                            start = LocalDateTime.of(2026, 4, 1, 10, 0),
                            end = LocalDateTime.of(2026, 4, 1, 12, 0),
                        )

                    every { sessionGroupRepository.findById(1L) } returns Optional.of(group)
                    every { sessionRepository.findAllBySessionGroupWithLock(group) } returns listOf(closedSession)

                    shouldNotThrowAny {
                        useCase.deleteGroup(clubId, 1L, userId, force = true)
                    }

                    verify(exactly = 1) { sessionGroupRepository.delete(group) }
                }
            }
        }
    })
