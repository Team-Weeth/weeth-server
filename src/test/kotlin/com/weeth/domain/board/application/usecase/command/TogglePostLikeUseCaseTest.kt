package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostLikeLockTimeoutException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostLikeTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.PessimisticLockingFailureException

class TogglePostLikeUseCaseTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val postLikeRepository = mockk<PostLikeRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val useCase = TogglePostLikeUseCase(postRepository, postLikeRepository, clubMemberPolicy)

        val clubId = 1L
        val userId = 10L
        val postId = 100L

        val club = ClubTestFixture.createClub(id = clubId)
        val board = BoardTestFixture.create(club = club)
        val member = ClubMemberTestFixture.createActiveMember(club = club)

        beforeTest {
            clearMocks(postRepository, postLikeRepository, clubMemberPolicy)
            every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
            every { postLikeRepository.save(any()) } answers { firstArg() }
        }

        describe("execute") {
            context("게시글이 존재하지 않을 때") {
                it("PostNotFoundException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } returns null

                    shouldThrow<PostNotFoundException> {
                        useCase.execute(clubId, postId, userId)
                    }
                }
            }

            context("락 획득에 실패했을 때") {
                it("PostLikeLockTimeoutException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } throws
                        PessimisticLockingFailureException("lock timeout")

                    shouldThrow<PostLikeLockTimeoutException> {
                        useCase.execute(clubId, postId, userId)
                    }
                }
            }

            context("다른 클럽의 게시글일 때") {
                it("PostNotFoundException을 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 99L)
                    val otherPost = PostTestFixture.create(board = BoardTestFixture.create(club = otherClub))
                    every { postRepository.findByIdWithLock(postId) } returns otherPost

                    shouldThrow<PostNotFoundException> {
                        useCase.execute(clubId, postId, userId)
                    }
                }
            }

            context("접근 권한이 없는 비공개 게시판일 때") {
                it("CategoryAccessDeniedException을 던진다") {
                    val privateBoard = BoardTestFixture.create(club = club, config = BoardConfig(isPrivate = true))
                    val privatePost = PostTestFixture.create(board = privateBoard)
                    val userMember = ClubMemberTestFixture.createActiveMember(club = club, memberRole = MemberRole.USER)
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns userMember
                    every { postRepository.findByIdWithLock(postId) } returns privatePost

                    shouldThrow<CategoryAccessDeniedException> {
                        useCase.execute(clubId, postId, userId)
                    }
                }
            }

            context("기존 좋아요가 없을 때") {
                it("새 PostLike를 생성하고 isLiked=true, likeCount=1을 반환한다") {
                    val post = PostTestFixture.create(board = board)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns null

                    val result = useCase.execute(clubId, postId, userId)

                    result.isLiked shouldBe true
                    result.likeCount shouldBe 1
                    verify(exactly = 1) { postLikeRepository.save(any()) }
                }
            }

            context("기존 좋아요(isActive=true)가 있을 때") {
                it("toggle하여 isLiked=false, likeCount를 감소시킨다") {
                    val post = PostTestFixture.create(board = board, initialLikeCount = 1)
                    val existingLike = PostLikeTestFixture.createActive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.execute(clubId, postId, userId)

                    result.isLiked shouldBe false
                    result.likeCount shouldBe 0
                    verify(exactly = 0) { postLikeRepository.save(any()) }
                }
            }

            context("기존 좋아요(isActive=false)가 있을 때") {
                it("toggle하여 isLiked=true, likeCount를 증가시킨다") {
                    val post = PostTestFixture.create(board = board)
                    val existingLike = PostLikeTestFixture.createInactive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.execute(clubId, postId, userId)

                    result.isLiked shouldBe true
                    result.likeCount shouldBe 1
                    verify(exactly = 0) { postLikeRepository.save(any()) }
                }
            }
        }
    })
