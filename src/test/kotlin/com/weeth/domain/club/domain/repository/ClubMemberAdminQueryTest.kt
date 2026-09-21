package com.weeth.domain.club.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

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
    private val entityManager: EntityManager,
) : DescribeSpec({

        val pageable = PageRequest.of(0, 10)
        val baseCreatedAt = LocalDateTime.of(2026, 3, 1, 10, 0)

        /**
         * 기수 없음 1명 + 6기 1명 + 6·7기 1명 + 대기 1명 + 추방 1명을 심고 clubId를 돌려준다.
         * 가입일은 저장 순서대로 1분씩 벌려 명시적으로 지정한다.
         */
        fun seed(): Long {
            val club = clubRepository.save(ClubTestFixture.createClub(code = "ADMINQ"))
            val cardinal6 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 6))
            val cardinal7 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 7))

            var emailSequence = 0
            val savedMemberIds = mutableListOf<Long>()

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
                savedMemberIds += member.id
            }

            save("가기수없음", "컴퓨터공학과", "20240001", MemberStatus.ACTIVE, emptyList())
            save("나육기", "컴퓨터공학과", "20240002", MemberStatus.ACTIVE, listOf(cardinal6))
            save("다칠기", "컴퓨터공학과", "20240003", MemberStatus.ACTIVE, listOf(cardinal6, cardinal7))
            save("라대기", null, null, MemberStatus.WAITING, emptyList())
            save("마추방", null, null, MemberStatus.BANNED, emptyList())

            // @CreatedDate로 찍히는 createdAt은 DB 컬럼 정밀도에 따라 연속 저장 시 같은 값이 될 수 있고,
            // 그러면 JOINED_DESC가 타이브레이커(id ASC)로 떨어져 검증이 무의미해진다.
            // createdAt은 updatable = false라 엔티티로는 못 바꾸므로 네이티브 UPDATE로 값을 확정한다.
            entityManager.flush()
            savedMemberIds.forEachIndexed { index, memberId ->
                entityManager
                    .createNativeQuery("UPDATE club_member SET created_at = :createdAt WHERE club_member_id = :id")
                    .setParameter("createdAt", baseCreatedAt.plusMinutes(index.toLong()))
                    .setParameter("id", memberId)
                    .executeUpdate()
            }
            // 영속성 컨텍스트에 남은 예전 값이 조회 결과로 되돌아오지 않도록 비운다.
            entityManager.clear()

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
                        null,
                        ClubMemberSort.CARDINAL_ASC.queryKey,
                        pageable,
                    )

                // 마추방(BANNED)은 기수와 무관하게 상태 우선순위로 맨 뒤로 밀리므로 별도로 확인한다.
                result.content.map { it.user.name }.dropLast(1) shouldContainExactly
                    listOf("가기수없음", "라대기", "나육기", "다칠기")
                result.content
                    .last()
                    .user.name shouldBe "마추방"
            }

            it("NAME_ASC는 이름순으로 정렬한다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
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
                        null,
                        ClubMemberSort.JOINED_DESC.queryKey,
                        pageable,
                    )

                // seed가 createdAt을 1분 간격으로 확정하므로 tie로 타이브레이커(id ASC)에 떨어질 일이 없다.
                // 값이 실제로 서로 다른지 먼저 확인해, 정밀도가 바뀌면 순서가 아니라 여기서 실패하게 한다.
                result.content.map { it.createdAt }.distinct() shouldHaveSize 5
                // 추방 멤버(마추방)는 가장 최근 가입임에도 상태 우선순위 때문에 맨 뒤로 밀린다.
                result.content.map { it.user.name } shouldContainExactly
                    listOf("라대기", "다칠기", "나육기", "가기수없음", "마추방")
            }

            it("탈퇴·추방 멤버는 정렬 기준과 무관하게 항상 맨 뒤로 정렬된다") {
                val clubId = seed()
                val club = clubRepository.findById(clubId).orElseThrow()
                val leftUser =
                    userRepository.save(
                        User(name = "가나다", email = Email.from("admin-query-left@test.com"), status = Status.ACTIVE),
                    )
                clubMemberRepository.save(ClubMember(club = club, user = leftUser, memberStatus = MemberStatus.LEFT))
                entityManager.flush()
                entityManager.clear()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        null,
                        null,
                        ClubMemberSort.NAME_ASC.queryKey,
                        PageRequest.of(0, 10),
                    )

                val deprioritized = setOf(MemberStatus.BANNED, MemberStatus.LEFT)
                val statuses = result.content.map { it.memberStatus }
                val firstDeprioritizedIndex = statuses.indexOfFirst { it in deprioritized }

                statuses.take(firstDeprioritizedIndex).none { it in deprioritized } shouldBe true
                statuses.drop(firstDeprioritizedIndex).all { it in deprioritized } shouldBe true
            }

            it("가입 대기·추방 멤버도 목록에 포함된다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
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
                    .findAdminMembers(clubId, null, null, "나육기", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 1
                clubMemberRepository
                    .findAdminMembers(clubId, null, null, "컴퓨터공학과", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 3
                clubMemberRepository
                    .findAdminMembers(clubId, null, null, "20240003", ClubMemberSort.CARDINAL_DESC.queryKey, pageable)
                    .totalElements shouldBe 1
            }

            it("기수 필터와 keyword가 동시에 적용된다") {
                val clubId = seed()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        6,
                        null,
                        "나",
                        ClubMemberSort.CARDINAL_DESC.queryKey,
                        pageable,
                    )

                result.totalElements shouldBe 1
                result.content
                    .single()
                    .user.name shouldBe "나육기"
            }

            it("역할 필터는 해당 역할을 가진 멤버만 반환한다") {
                val clubId = seed()
                val admin =
                    clubMemberRepository
                        .findAdminMembers(
                            clubId,
                            null,
                            null,
                            "가기수없음",
                            ClubMemberSort.NAME_ASC.queryKey,
                            pageable,
                        ).content
                        .single()
                admin.updateRole(MemberRole.ADMIN)
                clubMemberRepository.save(admin)
                entityManager.flush()
                entityManager.clear()

                val result =
                    clubMemberRepository.findAdminMembers(
                        clubId,
                        null,
                        MemberRole.ADMIN,
                        null,
                        ClubMemberSort.NAME_ASC.queryKey,
                        pageable,
                    )

                result.totalElements shouldBe 1
                result.content
                    .single()
                    .user.name shouldBe "가기수없음"
            }

            it("페이지 경계에서 멤버가 중복되거나 누락되지 않는다") {
                val clubId = seed()

                val pages =
                    (0..2).map {
                        clubMemberRepository.findAdminMembers(
                            clubId,
                            null,
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
                        .findAdminMembers(clubId, null, null, "마추방", ClubMemberSort.NAME_ASC.queryKey, pageable)
                        .content
                        .single()

                val found = clubMemberRepository.findAdminMemberDetail(banned.id)

                found?.id shouldBe banned.id
                found?.memberStatus shouldBe MemberStatus.BANNED
            }
        }
    })
