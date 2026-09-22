package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.domain.user.application.mapper.UserPostMapper
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class GetUserPostQueryServiceTest :
    DescribeSpec({
        val clock = Clock.fixed(Instant.parse("2026-06-30T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val postReader = mockk<PostReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val userPostMapper = UserPostMapper()
        val queryService =
            GetUserPostQueryService(
                postReader = postReader,
                clubMemberPolicy = clubMemberPolicy,
                userPostMapper = userPostMapper,
                clock = clock,
            )

        beforeTest {
            clearMocks(postReader, clubMemberPolicy)
        }

        describe("getMyPosts") {
            it("로그인 사용자가 작성한 게시글을 무한스크롤 목록으로 조회한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val club = ClubTestFixture.createClub(id = 100L, name = "Leets")
                val board = BoardTestFixture.create(id = 10L, club = club, name = "자유게시판")
                val member = ClubTestFixture.createClubMember(club = club, user = user)
                val createdAt = LocalDateTime.of(2026, 6, 29, 10, 0)
                val content = "내용".repeat(60)
                val post =
                    PostTestFixture
                        .create(
                            title = "제목",
                            content = content,
                            clubMember = member,
                            board = board,
                            initialLikeCount = 5,
                        ).withId(200L)
                        .withCreatedAt(createdAt)
                repeat(3) { post.increaseCommentCount() }
                val pageable = PageRequest.of(0, 5)
                every { clubMemberPolicy.getActiveMember(100L, 1L) } returns member
                every { postReader.findMyActivePosts(1L, 100L, pageable) } returns
                    SliceImpl(listOf(post), pageable, true)

                val result = queryService.getMyPosts(userId = 1L, clubId = 100L, pageNumber = 0, pageSize = 5)

                result.content shouldHaveSize 1
                result.content[0].postId shouldBe 200L
                result.content[0].clubId shouldBe TsidBase62Encoder.encode(100L)
                result.content[0].clubName shouldBe "Leets"
                result.content[0].boardId shouldBe 10L
                result.content[0].boardName shouldBe "자유게시판"
                result.content[0].title shouldBe "제목"
                result.content[0].content shouldBe content
                result.content[0].commentCount shouldBe 3
                result.content[0].likeCount shouldBe 5
                result.content[0].createdAt shouldBe createdAt
                result.content[0].isNew shouldBe false
                result.pageNumber shouldBe 0
                result.pageSize shouldBe 5
                result.numberOfElements shouldBe 1
                result.hasNext shouldBe true
            }

            it("pageNumber가 음수면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getMyPosts(userId = 1L, clubId = 100L, pageNumber = -1, pageSize = 5)
                }
            }

            it("pageSize가 0이면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getMyPosts(userId = 1L, clubId = 100L, pageNumber = 0, pageSize = 0)
                }
            }

            it("pageSize가 최대값을 초과하면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getMyPosts(userId = 1L, clubId = 100L, pageNumber = 0, pageSize = 51)
                }
            }
        }

        describe("getMemberPosts") {
            val club = ClubTestFixture.createClub(id = 100L, name = "Leets")
            val board = BoardTestFixture.create(id = 10L, club = club, name = "자유게시판")

            context("활성 멤버의 게시글을 조회하는 경우") {
                it("게시글 목록을 SliceResponse로 반환한다") {
                    val requester = ClubTestFixture.createClubMember(club = club)
                    val targetMember =
                        ClubTestFixture.createClubMember(
                            club = club,
                            user = UserTestFixture.createActiveUser1(2L),
                        )
                    val targetMemberId = 55L
                    ReflectionTestUtils.setField(targetMember, "id", targetMemberId)
                    val post =
                        PostTestFixture
                            .create(
                                title = "제목",
                                content = "내용",
                                clubMember = targetMember,
                                board = board,
                            ).withId(200L)
                    val pageable = PageRequest.of(0, 20)

                    every { clubMemberPolicy.getActiveMember(100L, 1L) } returns requester
                    every { clubMemberPolicy.getMemberInClub(100L, targetMemberId) } returns targetMember
                    every { postReader.findActivePostsByClubMemberId(targetMemberId, pageable) } returns
                        SliceImpl(listOf(post), pageable, false)

                    val result =
                        queryService.getMemberPosts(
                            requesterId = 1L,
                            clubId = 100L,
                            targetClubMemberId = targetMemberId,
                            pageNumber = 0,
                            pageSize = 20,
                        )

                    result.content shouldHaveSize 1
                    result.hasNext shouldBe false
                }
            }

            context("대상 멤버가 비활성인 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    val requester = ClubTestFixture.createClubMember(club = club)
                    val bannedMember = ClubMemberTestFixture.createBannedMember(club = club)

                    every { clubMemberPolicy.getActiveMember(100L, 1L) } returns requester
                    every { clubMemberPolicy.getMemberInClub(100L, 55L) } returns bannedMember

                    shouldThrow<ClubMemberNotFoundException> {
                        queryService.getMemberPosts(
                            requesterId = 1L,
                            clubId = 100L,
                            targetClubMemberId = 55L,
                            pageNumber = 0,
                            pageSize = 20,
                        )
                    }
                }
            }

            context("호출자가 비활성인 경우") {
                it("MemberNotActiveException을 던진다") {
                    every { clubMemberPolicy.getActiveMember(100L, 1L) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        queryService.getMemberPosts(
                            requesterId = 1L,
                            clubId = 100L,
                            targetClubMemberId = 55L,
                            pageNumber = 0,
                            pageSize = 20,
                        )
                    }
                }
            }
        }
    }) {
    companion object {
        private fun Post.withId(id: Long): Post =
            apply {
                ReflectionTestUtils.setField(this, "id", id)
            }

        private fun Post.withCreatedAt(createdAt: LocalDateTime): Post =
            apply {
                ReflectionTestUtils.setField(this, "createdAt", createdAt)
            }
    }
}
