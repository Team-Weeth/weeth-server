package com.weeth.domain.penalty.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PenaltyRepositoryTest(
    private val penaltyRepository: PenaltyRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val cardinalRepository: CardinalRepository,
    private val userRepository: UserRepository,
) : DescribeSpec({

        /**
         * 6기 페널티 1건·경고 1건 + 7기 페널티 1건을 심은 멤버를 만들어 (memberId, cardinal6Id, cardinal7Id)를 돌려준다.
         */
        fun seed(): Triple<Long, Long, Long> {
            val club = clubRepository.save(ClubTestFixture.createClub(code = "PENALTYQ"))
            val cardinal6 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 6))
            val cardinal7 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 7))
            val user =
                userRepository.save(
                    User(name = "홍길동", email = Email.from("penalty-query@test.com"), status = Status.ACTIVE),
                )
            val member = clubMemberRepository.save(ClubMember(club = club, user = user))

            penaltyRepository.save(
                Penalty(
                    clubMember = member,
                    cardinal = cardinal6,
                    penaltyDescription = "6기 페널티",
                    penaltyType = PenaltyType.PENALTY,
                ),
            )
            penaltyRepository.save(
                Penalty(
                    clubMember = member,
                    cardinal = cardinal6,
                    penaltyDescription = "6기 경고",
                    penaltyType = PenaltyType.WARNING,
                ),
            )
            penaltyRepository.save(
                Penalty(
                    clubMember = member,
                    cardinal = cardinal7,
                    penaltyDescription = "7기 페널티",
                    penaltyType = PenaltyType.PENALTY,
                ),
            )

            return Triple(member.id, cardinal6.id, cardinal7.id)
        }

        describe("countByClubMemberIdAndCardinalIdAndPenaltyType") {
            it("cardinalId가 null이면 전체 기수를 대상으로 센다") {
                val (memberId, _, _) = seed()

                penaltyRepository.countByClubMemberIdAndCardinalIdAndPenaltyType(
                    memberId,
                    null,
                    PenaltyType.PENALTY,
                ) shouldBe 2
                penaltyRepository.countByClubMemberIdAndCardinalIdAndPenaltyType(
                    memberId,
                    null,
                    PenaltyType.WARNING,
                ) shouldBe 1
            }

            it("cardinalId를 지정하면 해당 기수만 센다") {
                val (memberId, cardinal6Id, cardinal7Id) = seed()

                penaltyRepository.countByClubMemberIdAndCardinalIdAndPenaltyType(
                    memberId,
                    cardinal6Id,
                    PenaltyType.PENALTY,
                ) shouldBe 1
                penaltyRepository.countByClubMemberIdAndCardinalIdAndPenaltyType(
                    memberId,
                    cardinal7Id,
                    PenaltyType.WARNING,
                ) shouldBe 0
            }
        }

        describe("findSliceByClubMemberId") {
            it("cardinalId가 null이면 전체 기수의 페널티를 최신순으로 반환한다") {
                val (memberId, _, _) = seed()

                val result = penaltyRepository.findSliceByClubMemberId(memberId, null, PageRequest.of(0, 10))

                result.numberOfElements shouldBe 3
                result.content.map { it.penaltyDescription } shouldContainExactlyInAnyOrder
                    listOf("6기 페널티", "6기 경고", "7기 페널티")
            }

            it("cardinalId를 지정하면 해당 기수의 페널티만 반환한다") {
                val (memberId, cardinal6Id, _) = seed()

                val result = penaltyRepository.findSliceByClubMemberId(memberId, cardinal6Id, PageRequest.of(0, 10))

                result.numberOfElements shouldBe 2
                result.content.map { it.penaltyDescription } shouldContainExactlyInAnyOrder
                    listOf("6기 페널티", "6기 경고")
            }
        }
    })
