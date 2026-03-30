package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.application.dto.request.SessionUpdateRequest
import com.weeth.domain.session.application.exception.ClosedSessionIncludedException
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.enums.UpdateScope
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.LocalTime

class UpdateSessionUseCaseTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val userReader = mockk<UserReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)

        val recurringSessionPolicy = RecurringSessionPolicy()
        val useCase = UpdateSessionUseCase(sessionRepository, userReader, clubPermissionPolicy, recurringSessionPolicy)

        val clubId = 1L
        val userId = 10L
        val club = ClubTestFixture.createClub(id = clubId)
        val user = UserTestFixture.createActiveUser1()

        beforeTest {
            clearMocks(sessionRepository, userReader, clubPermissionPolicy)
            every { userReader.getById(userId) } returns user
        }

        describe("update") {
            context("존재하지 않는 세션") {
                it("예외를 던진다") {
                    every { sessionRepository.findByIdWithLock(99L) } returns null
                    val request = SessionUpdateRequest("변경", null, null, null, null)

                    shouldThrow<SessionNotFoundException> {
                        useCase.update(clubId, 99L, request, userId)
                    }
                }
            }

            context("다른 클럽의 세션") {
                it("예외를 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 999L)
                    val session = SessionTestFixture.createSession(id = 1L, club = otherClub)
                    every { sessionRepository.findByIdWithLock(1L) } returns session

                    val request = SessionUpdateRequest("변경", null, null, null, null)

                    shouldThrow<SessionNotFoundException> {
                        useCase.update(clubId, 1L, request, userId)
                    }
                }
            }

            context("THIS_ONLY 수정") {
                it("단일 세션의 제목과 내용을 수정한다") {
                    val session = SessionTestFixture.createSession(id = 1L, club = club)
                    every { sessionRepository.findByIdWithLock(1L) } returns session

                    val request = SessionUpdateRequest("변경된 제목", "변경된 내용", null, null, null)

                    useCase.update(clubId, 1L, request, userId)

                    session.title shouldBe "변경된 제목"
                    session.content shouldBe "변경된 내용"
                }

                it("반복 세션이어도 THIS_ONLY이면 해당 세션만 수정한다") {
                    val group = SessionTestFixture.createSessionGroup(id = 1L)
                    val session = SessionTestFixture.createSession(id = 1L, club = club, sessionGroup = group)
                    every { sessionRepository.findByIdWithLock(1L) } returns session

                    val request = SessionUpdateRequest("개별 변경", null, null, null, null)

                    useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_ONLY)

                    session.title shouldBe "개별 변경"
                }
            }

            context("THIS_AND_FUTURE 수정") {
                it("이후 모든 세션의 시간 부분만 변경된다 (날짜는 유지)") {
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

                    val request =
                        SessionUpdateRequest(
                            title = "통합 수정",
                            content = null,
                            location = null,
                            start = LocalDateTime.of(2026, 4, 1, 14, 0), // 시간만 14시로 변경
                            end = LocalDateTime.of(2026, 4, 1, 16, 0),
                        )

                    useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_AND_FUTURE)

                    // 날짜는 각각 유지, 시간만 변경
                    session1.start shouldBe LocalDateTime.of(2026, 4, 1, 14, 0)
                    session1.end shouldBe LocalDateTime.of(2026, 4, 1, 16, 0)
                    session2.start shouldBe LocalDateTime.of(2026, 4, 8, 14, 0)
                    session2.end shouldBe LocalDateTime.of(2026, 4, 8, 16, 0)

                    // 제목도 일괄 변경
                    session1.title shouldBe "통합 수정"
                    session2.title shouldBe "통합 수정"
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

                    val request = SessionUpdateRequest("수정", null, null, null, null)

                    shouldThrow<ClosedSessionIncludedException> {
                        useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_AND_FUTURE, force = false)
                    }
                }

                it("CLOSED 세션 포함 시 force=true이면 정상 수정된다") {
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

                    val request = SessionUpdateRequest("강제 수정", null, null, null, null)

                    shouldNotThrowAny {
                        useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_AND_FUTURE, force = true)
                    }

                    openSession.title shouldBe "강제 수정"
                    closedSession.title shouldBe "강제 수정"
                }

                it("시간 변경이 null이면 각 세션의 기존 시간을 유지한다") {
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

                    // 시간은 null, 제목만 변경
                    val request = SessionUpdateRequest("제목만 변경", null, null, null, null)

                    useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_AND_FUTURE)

                    session1.start.toLocalTime() shouldBe LocalTime.of(10, 0)
                    session2.start.toLocalTime() shouldBe LocalTime.of(10, 0)
                }

                it("자정을 넘는 시간으로 변경하면 end 날짜가 다음날로 설정된다") {
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

                    // 22:00~02:00(다음날)로 변경
                    val request =
                        SessionUpdateRequest(
                            title = null,
                            content = null,
                            location = null,
                            start = LocalDateTime.of(2026, 4, 1, 22, 0),
                            end = LocalDateTime.of(2026, 4, 2, 2, 0),
                        )

                    useCase.update(clubId, 1L, request, userId, scope = UpdateScope.THIS_AND_FUTURE)

                    session1.start shouldBe LocalDateTime.of(2026, 4, 1, 22, 0)
                    session1.end shouldBe LocalDateTime.of(2026, 4, 2, 2, 0)
                    session2.start shouldBe LocalDateTime.of(2026, 4, 8, 22, 0)
                    session2.end shouldBe LocalDateTime.of(2026, 4, 9, 2, 0)
                }
            }
        }
    })
