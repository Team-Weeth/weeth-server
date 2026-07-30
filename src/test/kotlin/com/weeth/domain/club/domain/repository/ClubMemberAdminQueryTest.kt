package com.weeth.domain.club.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest

/**
 * findAdminMembers는 기수 정렬을 위해 ORDER BY에 스칼라 서브쿼리를 쓰므로 실제 DB로 검증한다.
 * 각 테스트는 자기 데이터를 직접 심고 트랜잭션 롤백에 의존한다.
 */
@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClubMemberAdminQueryTest(
    private val clubMemberRepository: ClubMemberRepository,
    private val clubRepository: ClubRepository,
    private val userRepository: UserRepository,
    private val cardinalRepository: CardinalRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
) : DescribeSpec({

        val pageable = PageRequest.of(0, 10)

        /**
         * 기수 없음 1명 + 6기 1명 + 6·7기 1명 + 대기 1명 + 추방 1명을 심고 clubId를 돌려준다.
         */
        fun seed(): Long {
            val club = clubRepository.save(ClubTestFixture.createClub(code = "ADMINQ"))
            val cardinal6 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 6))
            val cardinal7 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 7))

            var emailSequence = 0

            fun save(
                name: String,
                department: String?,
                studentId: String?,
                status: MemberStatus,
                cardinals: List<Cardinal>,
            ) {
                val user =
                    userRepository.save(
                        User(
                            name = name,
                            email = Email.from("admin-query-${emailSequence++}@test.com"),
                            department = department,
                            studentId = studentId,
                            status = Status.ACTIVE,
                        ),
                    )
                val member =
                    clubMemberRepository.save(ClubMember(club = club, user = user, memberStatus = status))
                cardinals.forEach { clubMemberCardinalRepository.save(ClubMemberCardinal.create(member, it)) }
            }

            save("가기수없음", "컴퓨터공학과", "20240001", MemberStatus.ACTIVE, emptyList())
            save("나육기", "컴퓨터공학과", "20240002", MemberStatus.ACTIVE, listOf(cardinal6))
            save("다칠기", "컴퓨터공학과", "20240003", MemberStatus.ACTIVE, listOf(cardinal6, cardinal7))
            save("라대기", null, null, MemberStatus.WAITING, emptyList())
            save("마추방", null, null, MemberStatus.BANNED, emptyList())
            return club.id
        }

        describe("findAdminMembers") {
            it("CARDINAL_DESC는 최신 기수 우선, 기수 없는 멤버는 뒤로 밀린다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        ClubMemberSort.CARDINAL_DESC.queryKey,
                        pageable,
                    )

                result.totalElements shouldBe 5
                result.content.take(2).map { it.user.name } shouldContainExactly listOf("다칠기", "나육기")
                // 기수가 없는 멤버끼리는 clubMemberId ASC 타이브레이커로 안정 정렬된다.
                result.content.drop(2).map { it.user.name } shouldContainExactly
                    listOf("가기수없음", "라대기", "마추방")
            }

            it("CARDINAL_ASC는 기수 오름차순으로 정렬한다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        ClubMemberSort.CARDINAL_ASC.queryKey,
                        pageable,
                    )

                result.content.takeLast(2).map { it.user.name } shouldContainExactly listOf("나육기", "다칠기")
            }

            it("NAME_ASC는 이름순으로 정렬한다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        ClubMemberSort.NAME_ASC.queryKey,
                        pageable,
                    )

                result.content.map { it.user.name } shouldContainExactly
                    listOf("가기수없음", "나육기", "다칠기", "라대기", "마추방")
            }

            it("JOINED_DESC는 최근 가입순으로 정렬한다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        ClubMemberSort.JOINED_DESC.queryKey,
                        pageable,
                    )

                // seed는 저장 순서대로 createdAt이 증가하므로 역순이어야 한다.
                result.content.map { it.user.name } shouldContainExactly
                    listOf("마추방", "라대기", "다칠기", "나육기", "가기수없음")
                result.content.map { it.createdAt } shouldBe result.content.map { it.createdAt }.sortedDescending()
            }

            it("가입 대기·추방 멤버도 목록에 포함된다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        ClubMemberSort.NAME_ASC.queryKey,
                        pageable,
                    )

                result.content.map { it.memberStatus } shouldContainExactlyInAnyOrder
                    listOf(
                        MemberStatus.ACTIVE,
                        MemberStatus.ACTIVE,
                        MemberStatus.ACTIVE,
                        MemberStatus.WAITING,
                        MemberStatus.BANNED,
                    )
            }

            it("기수 필터는 해당 기수를 가진 멤버만 반환한다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        7,
                        null,
                        ClubMemberSort.CARDINAL_DESC.queryKey,
                        pageable,
                    )

                result.totalElements shouldBe 1
                result.content
                    .single()
                    .user.name shouldBe "다칠기"
            }

            it("keyword는 이름·학과·학번을 대상으로 검색한다") {
                val clubId = seed()

                clubMemberRepository
                    .findAdminMembers(clubId, null, "나육기", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 1
                clubMemberRepository
                    .findAdminMembers(clubId, null, "컴퓨터공학과", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 3
                clubMemberRepository
                    .findAdminMembers(clubId, null, "20240003", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 1
            }

            it("기수 필터와 keyword가 동시에 적용된다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        6,
                        "나",
                        ClubMemberSort.CARDINAL_DESC.queryKey,
                        pageable,
                    )

                result.totalElements shouldBe 1
                result.content
                    .single()
                    .user.name shouldBe "나육기"
            }

            it("페이지 경계에서 멤버가 중복되거나 누락되지 않는다") {
                val clubId = seed()

                val pages =
                    (0..2).map {
                        clubMemberRepository.findAdminMembers(
                            clubId,
                            null,
                            null,
                            ClubMemberSort.CARDINAL_DESC.queryKey,
                            PageRequest.of(it, 2),
                        )
                    }

                pages.first().totalPages shouldBe 3
                pages.flatMap { page -> page.content.map { it.id } }.distinct().size shouldBe 5
            }
        }

        describe("findAdminMemberDetail") {
            it("상태와 무관하게 단건 조회된다") {
                val clubId = seed()
                val banned =
                    clubMemberRepository
                        .findAdminMembers(clubId, null, "마추방", ClubMemberSort.NAME_ASC.queryKey, pageable)
                        .content
                        .single()

                val found = clubMemberRepository.findAdminMemberDetail(banned.id)

                found?.id shouldBe banned.id
                found?.memberStatus shouldBe MemberStatus.BANNED
            }
        }
    })
