package com.weeth.domain.club.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClubMemberNotificationTargetQueryTest(
    private val clubMemberRepository: ClubMemberRepository,
    private val clubRepository: ClubRepository,
    private val userRepository: UserRepository,
    private val cardinalRepository: CardinalRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
) : StringSpec({

        "진행 중인 기수 또는 공지 기수에 속한 활성 회원만 조회한다" {
            val club = clubRepository.save(ClubTestFixture.createClub(code = "NOTICE_TARGET"))
            val mappedCardinal =
                cardinalRepository.save(Cardinal.create(club, cardinalNumber = 6, status = CardinalStatus.DONE))
            val currentCardinal =
                cardinalRepository.save(
                    Cardinal.create(club, cardinalNumber = 7, status = CardinalStatus.IN_PROGRESS),
                )
            val unrelatedCardinal =
                cardinalRepository.save(Cardinal.create(club, cardinalNumber = 8, status = CardinalStatus.DONE))

            var emailSequence = 0

            fun saveMember(
                status: MemberStatus,
                cardinals: List<Cardinal>,
            ): ClubMember {
                val user =
                    userRepository.save(
                        User(
                            name = "알림 대상 $emailSequence",
                            email = Email.from("notice-target-${emailSequence++}@test.com"),
                            status = Status.ACTIVE,
                        ),
                    )
                val member = clubMemberRepository.save(ClubMember(club, user, memberStatus = status))
                cardinals.forEach {
                    clubMemberCardinalRepository.save(ClubMemberCardinal.create(member, it))
                }
                return member
            }

            val author = saveMember(MemberStatus.ACTIVE, listOf(currentCardinal))
            val currentMember = saveMember(MemberStatus.ACTIVE, listOf(currentCardinal))
            val mappedMember = saveMember(MemberStatus.ACTIVE, listOf(mappedCardinal))
            val bothMember = saveMember(MemberStatus.ACTIVE, listOf(mappedCardinal, currentCardinal))
            saveMember(MemberStatus.ACTIVE, listOf(unrelatedCardinal))
            saveMember(MemberStatus.WAITING, listOf(currentCardinal))

            val result =
                clubMemberRepository.findActiveUserIdsByClubIdExcludingUserId(
                    clubId = club.id,
                    cardinalNumber = mappedCardinal.cardinalNumber,
                    excludedUserId = author.user.id,
                )

            result shouldContainExactlyInAnyOrder
                listOf(currentMember.user.id, mappedMember.user.id, bothMember.user.id)
        }
    })
