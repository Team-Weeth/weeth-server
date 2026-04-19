package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostLikeLockTimeoutException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Post
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

class ManagePostLikeUseCaseTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val postLikeRepository = mockk<PostLikeRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val postMapper = mockk<PostMapper>(relaxed = true)
        val useCase = ManagePostLikeUseCase(postRepository, postLikeRepository, clubMemberPolicy, postMapper)

        val clubId = 1L
        val userId = 10L
        val postId = 100L

        val club = ClubTestFixture.createClub(id = clubId)
        val board = BoardTestFixture.create(club = club)
        val member = ClubMemberTestFixture.createActiveMember(club = club)

        beforeTest {
            clearMocks(postRepository, postLikeRepository, clubMemberPolicy, postMapper)
            every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
            every { postLikeRepository.save(any()) } answers { firstArg() }
            every { postMapper.toLikeResponse(any(), any()) } answers {
                PostLikeResponse(isLiked = secondArg(), likeCount = firstArg<Post>().likeCount)
            }
        }

        describe("like") {
            context("게시글이 존재하지 않을 때") {
                it("PostNotFoundException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } returns null

                    shouldThrow<PostNotFoundException> { useCase.like(clubId, postId, userId) }
                }
            }

            context("락 획득에 실패했을 때") {
                it("PostLikeLockTimeoutException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } throws
                        PessimisticLockingFailureException("lock timeout")

                    shouldThrow<PostLikeLockTimeoutException> { useCase.like(clubId, postId, userId) }
                }
            }

            context("다른 클럽의 게시글일 때") {
                it("PostNotFoundException을 던진다") {
                    val otherPost =
                        PostTestFixture.create(
                            board = BoardTestFixture.create(club = ClubTestFixture.createClub(id = 99L)),
                        )
                    every { postRepository.findByIdWithLock(postId) } returns otherPost

                    shouldThrow<PostNotFoundException> { useCase.like(clubId, postId, userId) }
                }
            }

            context("접근 권한이 없는 비공개 게시판일 때") {
                it("CategoryAccessDeniedException을 던진다") {
                    val privatePost =
                        PostTestFixture.create(
                            board = BoardTestFixture.create(club = club, config = BoardConfig(isPrivate = true)),
                        )
                    val userMember = ClubMemberTestFixture.createActiveMember(club = club, memberRole = MemberRole.USER)
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns userMember
                    every { postRepository.findByIdWithLock(postId) } returns privatePost

                    shouldThrow<CategoryAccessDeniedException> { useCase.like(clubId, postId, userId) }
                }
            }

            context("PostLike가 없을 때") {
                it("새 PostLike를 생성하고 likeCount를 증가시킨다") {
                    val post = PostTestFixture.create(board = board)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns null

                    val result = useCase.like(clubId, postId, userId)

                    result.isLiked shouldBe true
                    result.likeCount shouldBe 1
                    verify(exactly = 1) { postLikeRepository.save(any()) }
                }
            }

            context("PostLike(isActive=false)가 있을 때") {
                it("activate하고 likeCount를 증가시킨다") {
                    val post = PostTestFixture.create(board = board)
                    val existingLike = PostLikeTestFixture.createInactive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.like(clubId, postId, userId)

                    result.isLiked shouldBe true
                    result.likeCount shouldBe 1
                    verify(exactly = 0) { postLikeRepository.save(any()) }
                }
            }

            context("PostLike(isActive=true)가 이미 있을 때") {
                it("no-op으로 isLiked=true를 그대로 반환한다") {
                    val post = PostTestFixture.create(board = board, initialLikeCount = 1)
                    val existingLike = PostLikeTestFixture.createActive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.like(clubId, postId, userId)

                    result.isLiked shouldBe true
                    result.likeCount shouldBe 1
                    verify(exactly = 0) { postLikeRepository.save(any()) }
                }
            }
        }

        describe("unlike") {
            context("게시글이 존재하지 않을 때") {
                it("PostNotFoundException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } returns null

                    shouldThrow<PostNotFoundException> { useCase.unlike(clubId, postId, userId) }
                }
            }

            context("락 획득에 실패했을 때") {
                it("PostLikeLockTimeoutException을 던진다") {
                    every { postRepository.findByIdWithLock(postId) } throws
                        PessimisticLockingFailureException("lock timeout")

                    shouldThrow<PostLikeLockTimeoutException> { useCase.unlike(clubId, postId, userId) }
                }
            }

            context("다른 클럽의 게시글일 때") {
                it("PostNotFoundException을 던진다") {
                    val otherPost =
                        PostTestFixture.create(
                            board = BoardTestFixture.create(club = ClubTestFixture.createClub(id = 99L)),
                        )
                    every { postRepository.findByIdWithLock(postId) } returns otherPost

                    shouldThrow<PostNotFoundException> { useCase.unlike(clubId, postId, userId) }
                }
            }

            context("접근 권한이 없는 비공개 게시판일 때") {
                it("CategoryAccessDeniedException을 던진다") {
                    val privatePost =
                        PostTestFixture.create(
                            board = BoardTestFixture.create(club = club, config = BoardConfig(isPrivate = true)),
                        )
                    val userMember = ClubMemberTestFixture.createActiveMember(club = club, memberRole = MemberRole.USER)
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns userMember
                    every { postRepository.findByIdWithLock(postId) } returns privatePost

                    shouldThrow<CategoryAccessDeniedException> { useCase.unlike(clubId, postId, userId) }
                }
            }

            context("PostLike(isActive=true)가 있을 때") {
                it("deactivate하고 likeCount를 감소시킨다") {
                    val post = PostTestFixture.create(board = board, initialLikeCount = 1)
                    val existingLike = PostLikeTestFixture.createActive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.unlike(clubId, postId, userId)

                    result.isLiked shouldBe false
                    result.likeCount shouldBe 0
                }
            }

            context("PostLike(isActive=false)가 있을 때") {
                it("no-op으로 isLiked=false를 그대로 반환한다") {
                    val post = PostTestFixture.create(board = board)
                    val existingLike = PostLikeTestFixture.createInactive(post = post, userId = userId)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns existingLike

                    val result = useCase.unlike(clubId, postId, userId)

                    result.isLiked shouldBe false
                    result.likeCount shouldBe 0
                }
            }

            context("PostLike가 없을 때") {
                it("no-op으로 isLiked=false를 반환한다") {
                    val post = PostTestFixture.create(board = board)
                    every { postRepository.findByIdWithLock(postId) } returns post
                    every { postLikeRepository.findByPostAndUserId(post, userId) } returns null

                    val result = useCase.unlike(clubId, postId, userId)

                    result.isLiked shouldBe false
                    result.likeCount shouldBe 0
                }
            }
        }
    })
