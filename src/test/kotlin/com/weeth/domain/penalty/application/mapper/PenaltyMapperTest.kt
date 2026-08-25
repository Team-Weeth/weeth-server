package com.weeth.domain.penalty.application.mapper

import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDateTime

class PenaltyMapperTest :
    DescribeSpec({
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val mapper = PenaltyMapper(fileAccessUrlPort)

        describe("toResponse") {
            it("멤버 상태를 응답에 포함한다") {
                val member =
                    ClubMemberTestFixture
                        .createActiveMember(user = UserTestFixture.createActiveUser2())
                        .also { it.leave(LocalDateTime.of(2026, 5, 19, 12, 0)) }
                val cardinal = CardinalTestFixture.createCardinal(club = member.club, cardinalNumber = 1)
                val penalty = Penalty(clubMember = member, cardinal = cardinal, penaltyDescription = "지각")

                val response = mapper.toResponse(member, listOf(penalty), emptyList())

                response.memberStatus shouldBe MemberStatus.LEFT
            }
        }
    })
