package com.weeth.domain.club.domain.service

import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostLikeTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileRepository
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
        val commentRepository = mockk<CommentRepository>()
        val fileRepository = mockk<FileRepository>()
        val policy = ClubActivityDeletionPolicy(postLikeRepository, postRepository, commentRepository, fileRepository)

        beforeTest {
            clearMocks(postLikeRepository, postRepository, commentRepository, fileRepository)
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
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(member.user.id, listOf(club.id))
                } returns listOf(post.id)
                every { postRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()
                every { commentRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()
                every { postRepository.findAllByIdsWithLock(listOf(post.id)) } returns listOf(post)
                every {
                    postLikeRepository.findAllActiveByUserIdAndPostIds(member.user.id, listOf(post.id))
                } returns listOf(like)

                policy.markMemberActivitiesDeleted(member, now)

                like.isActive shouldBe false
                like.deletedAt shouldBe now
                post.likeCount shouldBe 0
                verify(exactly = 1) {
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(member.user.id, listOf(club.id))
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
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(member.user.id, listOf(club.id))
                } returns emptyList()
                every { postRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()
                every { commentRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()

                policy.markMemberActivitiesDeleted(member, LocalDateTime.of(2026, 5, 19, 12, 0))

                verify(exactly = 0) { postRepository.findAllByIdsWithLock(any()) }
                verify(exactly = 0) { postLikeRepository.findAllActiveByUserIdAndPostIds(any(), any()) }
            }

            it("삭제된 게시글에 남은 활성 좋아요도 삭제 마킹한다") {
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
                post.markDeleted()
                val like = PostLikeTestFixture.createActive(post = post, userId = member.user.id)
                val now = LocalDateTime.of(2026, 5, 19, 12, 0)

                every {
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(member.user.id, listOf(club.id))
                } returns listOf(post.id)
                every { postRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()
                every { commentRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns emptyList()
                every { postRepository.findAllByIdsWithLock(listOf(post.id)) } returns emptyList()
                every {
                    postLikeRepository.findAllActiveByUserIdAndPostIds(member.user.id, listOf(post.id))
                } returns listOf(like)

                policy.markMemberActivitiesDeleted(member, now)

                like.isActive shouldBe false
                like.deletedAt shouldBe now
                post.likeCount shouldBe 1
                verify(exactly = 1) { postRepository.findAllByIdsWithLock(listOf(post.id)) }
                verify(exactly = 1) {
                    postLikeRepository.findAllActiveByUserIdAndPostIds(member.user.id, listOf(post.id))
                }
            }

            it("작성한 게시글과 댓글의 파일을 하드 딜리트한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member =
                    ClubMemberTestFixture.createActiveMember(
                        id = 10L,
                        club = club,
                        user = UserTestFixture.createActiveUser1(id = 20L),
                    )
                val now = LocalDateTime.of(2026, 5, 19, 12, 0)

                every {
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(member.user.id, listOf(club.id))
                } returns emptyList()
                every { postRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns
                    listOf(100L, 101L)
                every { commentRepository.findActiveIdsByClubMemberIdIn(listOf(member.id)) } returns
                    listOf(200L)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.POST, listOf(100L, 101L))
                } returns 1
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.COMMENT, listOf(200L))
                } returns 1

                policy.markMemberActivitiesDeleted(member, now)

                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.POST, listOf(100L, 101L))
                }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.COMMENT, listOf(200L))
                }
                verify(exactly = 0) { postRepository.findAllByIdsWithLock(any()) }
            }
        }

        describe("markMembersActivitiesDeleted") {
            it("여러 멤버의 게시글과 댓글 파일을 ownerType별 한 번씩 하드 딜리트한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val user = UserTestFixture.createActiveUser1(id = 20L)
                val firstMember =
                    ClubMemberTestFixture.createActiveMember(
                        id = 10L,
                        club = club,
                        user = user,
                    )
                val secondMember =
                    ClubMemberTestFixture.createActiveMember(
                        id = 11L,
                        club = ClubTestFixture.createClub(id = 2L),
                        user = user,
                    )
                val now = LocalDateTime.of(2026, 5, 19, 12, 0)

                every { postRepository.findActiveIdsByClubMemberIdIn(listOf(10L, 11L)) } returns listOf(100L, 101L)
                every { commentRepository.findActiveIdsByClubMemberIdIn(listOf(10L, 11L)) } returns listOf(200L)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.POST, listOf(100L, 101L))
                } returns 1
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.COMMENT, listOf(200L))
                } returns 1
                every {
                    postLikeRepository.findActivePostIdsByUserIdAndClubIdIn(user.id, listOf(1L, 2L))
                } returns emptyList()

                policy.markMembersActivitiesDeleted(listOf(firstMember, secondMember), now)

                verify(exactly = 1) { postRepository.findActiveIdsByClubMemberIdIn(listOf(10L, 11L)) }
                verify(exactly = 1) { commentRepository.findActiveIdsByClubMemberIdIn(listOf(10L, 11L)) }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.POST, listOf(100L, 101L))
                }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.COMMENT, listOf(200L))
                }
            }
        }
    })
