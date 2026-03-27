package com.weeth.domain.session.application.usecase.query

import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.session.application.dto.response.SessionResponse
import com.weeth.domain.session.application.dto.response.SessionInfosResponse
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.application.mapper.SessionMapper
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetSessionQueryServiceTest :
    DescribeSpec({
        val sessionRepository = mockk<SessionRepository>()
        val cardinalReader = mockk<CardinalReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val sessionMapper = mockk<SessionMapper>()
        val queryService =
            GetSessionQueryService(
                sessionRepository,
                cardinalReader,
                clubMemberPolicy,
                clubPermissionPolicy,
                sessionMapper,
            )

        val clubId = 1L
        val userId = 10L

        beforeTest {
            clearMocks(sessionRepository, cardinalReader, clubMemberPolicy, clubPermissionPolicy, sessionMapper)
        }

        describe("findSession") {
            it("존재하지 않는 세션이면 예외를 던진다") {
                every { sessionRepository.findByIdAndClubId(99L, clubId) } returns null

                shouldThrow<SessionNotFoundException> {
                    queryService.findSession(clubId, userId, 99L)
                }
            }

            it("어드민/리드는 admin 응답을 반환한다") {
                val session = SessionTestFixture.createSession()
                val adminMember = ClubMemberTestFixture.createAdminMember()
                val response = mockk<SessionResponse>()

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns adminMember
                every { sessionRepository.findByIdAndClubId(1L, clubId) } returns session
                every { sessionMapper.toAdminResponse(session) } returns response

                val result = queryService.findSession(clubId, userId, 1L)

                result shouldBe response
                verify(exactly = 0) { sessionMapper.toResponse(any()) }
            }

            it("일반 멤버는 일반 응답을 반환한다") {
                val session = SessionTestFixture.createSession()
                val member = ClubMemberTestFixture.createActiveMember()
                val response = mockk<SessionResponse>()

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { sessionRepository.findByIdAndClubId(1L, clubId) } returns session
                every { sessionMapper.toResponse(session) } returns response

                val result = queryService.findSession(clubId, userId, 1L)

                result shouldBe response
                verify(exactly = 0) { sessionMapper.toAdminResponse(any()) }
            }
        }

        describe("findSessionInfos") {
            it("cardinal이 null이면 클럽 전체 세션을 반환한다") {
                val sessions = listOf(SessionTestFixture.createSession())
                val response = mockk<SessionInfosResponse>()

                every { sessionRepository.findAllByClubIdOrderByStartDesc(clubId) } returns sessions
                every { sessionMapper.toSingleGroupResponse(any()) } returns mockk(relaxed = true)
                every { sessionMapper.toInfos(any(), any()) } returns response

                val result = queryService.findSessionInfos(clubId, userId, null)

                result shouldBe response
                verify(exactly = 0) { cardinalReader.findByClubIdAndCardinalNumber(any(), any()) }
            }

            it("cardinal이 지정되면 해당 기수의 세션만 반환한다") {
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 3, year = 2026, semester = 1)
                val sessions = listOf(SessionTestFixture.createSession(cardinal = 3))
                val response = mockk<SessionInfosResponse>()

                every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 3) } returns cardinal
                every { sessionRepository.findAllByClubIdAndCardinalOrderByStartDesc(clubId, 3) } returns sessions
                every { sessionMapper.toSingleGroupResponse(any()) } returns mockk(relaxed = true)
                every { sessionMapper.toInfos(any(), any()) } returns response

                val result = queryService.findSessionInfos(clubId, userId, 3)

                result shouldBe response
            }

            it("존재하지 않는 기수를 요청하면 예외를 던진다") {
                every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 99) } returns null

                shouldThrow<CardinalNotFoundException> {
                    queryService.findSessionInfos(clubId, userId, 99)
                }
                verify(exactly = 0) { sessionRepository.findAllByClubIdAndCardinalOrderByStartDesc(any(), any()) }
            }
        }
    })
