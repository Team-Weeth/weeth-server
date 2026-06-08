package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostLikeTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class ClubActivityDeletionPolicyTest :
    DescribeSpec({
        val postLikeRepository = mockk<PostLikeRepository>()
        val postRepository = mockk<PostRepository>()
        val policy = ClubActivityDeletionPolicy(postLikeRepository, postRepository)

        beforeTest {
            clearMocks(postLikeRepository, postRepository)
        }

        describe("markMemberActivitiesDeleted") {
            it("멤버의 활성 좋아요를 삭제 마킹하고 게시글 좋아요 수를 감소시킨다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member =
                    ClubMemberTestFixture.createActiveMember(
                        club = club,
                        user = UserTestFixture.createActiveUser1(id = 10L),
                    )
                val post =
                    PostTestFixture.create(
                        board = BoardTestFixture.create(club = club),
                        initialLikeCount = 1,
                    )
                ReflectionTestUtils.setField(post, "id", 100L)
                val like = PostLikeTestFixture.createActive(post = post, userId = member.user.id)
                val now = LocalDateTime.of(2026, 5, 19, 12, 0)

                every {
                    postLikeRepository.findActivePostIdsByUserIdAndClubId(member.user.id, club.id)
                } returns listOf(post.id)
                every { postRepository.findAllByIdsWithLock(listOf(post.id)) } returns listOf(post)
                every {
                    postLikeRepository.findAllActiveByUserIdAndPostIds(member.user.id, listOf(post.id))
                } returns listOf(like)

                policy.markMemberActivitiesDeleted(member, now)

                like.isActive shouldBe false
                like.deletedAt shouldBe now
                post.likeCount shouldBe 0
                verify(exactly = 1) {
                    postLikeRepository.findActivePostIdsByUserIdAndClubId(member.user.id, club.id)
                }
                verify(exactly = 1) {
                    postRepository.findAllByIdsWithLock(listOf(post.id))
                }
                verify(exactly = 1) {
                    postLikeRepository.findAllActiveByUserIdAndPostIds(member.user.id, listOf(post.id))
                }
            }

            it("삭제 대상 좋아요가 없으면 게시글 락 조회를 수행하지 않는다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member =
                    ClubMemberTestFixture.createActiveMember(
                        club = club,
                        user = UserTestFixture.createActiveUser1(id = 10L),
                    )

                every {
                    postLikeRepository.findActivePostIdsByUserIdAndClubId(member.user.id, club.id)
                } returns emptyList()

                policy.markMemberActivitiesDeleted(member, LocalDateTime.of(2026, 5, 19, 12, 0))

                verify(exactly = 0) { postRepository.findAllByIdsWithLock(any()) }
                verify(exactly = 0) { postLikeRepository.findAllActiveByUserIdAndPostIds(any(), any()) }
            }
        }
    })
