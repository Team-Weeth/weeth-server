package com.weeth.domain.comment.application.usecase.command

import com.weeth.config.QueryCountUtil
import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.domain.entity.enums.Part
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class, CommentConcurrencyBenchmarkConfig::class)
class CommentConcurrencyTest(
    private val postCommentUsecase: PostCommentUsecase,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val entityManager: EntityManager,
    private val atomicCommentCountCommand: AtomicCommentCountCommand,
) : DescribeSpec({
        val runPerformanceTests = System.getProperty("runPerformanceTests")?.toBoolean() ?: false

        data class ConcurrencyResult(
            val successCount: Int,
            val failCount: Int,
            val postCommentCount: Int,
            val actualCommentCount: Int,
            val queryCount: Long,
            val elapsedTimeMs: Double,
            val firstError: String?,
        )

        fun createUsers(size: Int): List<User> =
            (1..size).map { i ->
                userRepository.save(
                    User
                        .builder()
                        .name("user$i")
                        .email("user$i@test.com")
                        .status(Status.ACTIVE)
                        .build(),
                )
            }

        fun createPost(title: String): Post =
            postRepository.save(
                Post
                    .builder()
                    .title(title)
                    .content("내용")
                    .comments(ArrayList())
                    .commentCount(0)
                    .category(Category.StudyLog)
                    .cardinalNumber(1)
                    .week(1)
                    .part(Part.ALL)
                    .parts(listOf(Part.ALL))
                    .build(),
            )

        fun runConcurrentSave(
            threadCount: Int,
            saveAction: (postId: Long, userId: Long, index: Int) -> Unit,
        ): ConcurrencyResult {
            val users = createUsers(threadCount)
            val post = createPost("동시성 테스트 게시글")
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val firstError = AtomicReference<String?>(null)

            entityManager.clear()

            val measured =
                QueryCountUtil.count(entityManager) {
                    repeat(threadCount) { i ->
                        executor.submit {
                            try {
                                saveAction(post.id, users[i].id, i)
                                successCount.incrementAndGet()
                            } catch (e: Exception) {
                                failCount.incrementAndGet()
                                firstError.compareAndSet(null, "${e::class.simpleName}: ${e.message}")
                            } finally {
                                latch.countDown()
                            }
                        }
                    }

                    latch.await()
                    executor.shutdown()
                }

            entityManager.clear()
            val updatedPost = postRepository.findById(post.id).orElseThrow()
            val actualCommentCount =
                entityManager
                    .createQuery("select count(c) from Comment c where c.post.id = :postId", java.lang.Long::class.java)
                    .setParameter("postId", post.id)
                    .singleResult
                    .toInt()

            return ConcurrencyResult(
                successCount = successCount.get(),
                failCount = failCount.get(),
                postCommentCount = updatedPost.commentCount,
                actualCommentCount = actualCommentCount,
                queryCount = measured.queryCount,
                elapsedTimeMs = measured.elapsedTimeMs,
                firstError = firstError.get(),
            )
        }

        afterEach {
            commentRepository.deleteAllInBatch()
            postRepository.deleteAllInBatch()
            userRepository.deleteAllInBatch()
        }

        describe("동시 댓글 생성") {
            it("10개의 동시 요청 후 commentCount가 정확히 10이어야 한다") {
                val threadCount = 10
                val result =
                    runConcurrentSave(threadCount) { postId, userId, index ->
                        postCommentUsecase.savePostComment(
                            dto = CommentSaveRequest(parentCommentId = null, content = "댓글 $index", files = null),
                            postId = postId,
                            userId = userId,
                        )
                    }
                result.successCount shouldBe threadCount
                result.failCount shouldBe 0
                result.postCommentCount shouldBe result.actualCommentCount
                result.postCommentCount shouldBe threadCount
                result.firstError shouldBe null
            }
        }

        describe("동시성 해소 방식별 성능 비교") {
            // TODO(board-refactor): Board 도메인 구조 개편(댓글 카운트 책임/저장 구조 변경) 이후
            // 이 비교 시나리오는 동일 조건으로 다시 측정해 기준선을 재작성한다.
            it("PESSIMISTIC_WRITE와 Atomic Increment를 측정한다").config(enabled = runPerformanceTests) {
                val threadCount = 30

                val pessimisticResult =
                    runConcurrentSave(threadCount) { postId, userId, index ->
                        postCommentUsecase.savePostComment(
                            dto =
                                CommentSaveRequest(
                                    parentCommentId = null,
                                    content = "pessimistic-$index",
                                    files = null,
                                ),
                            postId = postId,
                            userId = userId,
                        )
                    }

                val atomicResult =
                    runConcurrentSave(threadCount) { postId, userId, index ->
                        atomicCommentCountCommand.savePostCommentWithAtomicIncrement(
                            dto =
                                CommentSaveRequest(
                                    parentCommentId = null,
                                    content = "atomic-$index",
                                    files = null,
                                ),
                            postId = postId,
                            userId = userId,
                        )
                    }

                println("[pessimistic] $pessimisticResult")
                println("[atomic] $atomicResult")

                pessimisticResult.failCount shouldBe 0
                atomicResult.failCount shouldBe 0
                pessimisticResult.postCommentCount shouldBe threadCount
                pessimisticResult.actualCommentCount shouldBe threadCount
                atomicResult.postCommentCount shouldBe threadCount
                atomicResult.actualCommentCount shouldBe threadCount
            }
        }
    })

class AtomicCommentCountCommand(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {
    // TODO(board-refactor): 현재는 동시성 비교 실험용 테스트 전용 커맨드.
    // Board 리팩토링 후 실제 카운트 갱신 구조에 맞춰 제거 또는 대체한다.
    fun savePostCommentWithAtomicIncrement(
        dto: CommentSaveRequest,
        postId: Long,
        userId: Long,
    ) {
        val maxRetries = 10
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val user = userRepository.findById(userId).orElseThrow()
                    val post = postRepository.findById(postId).orElseThrow()
                    val parent =
                        dto.parentCommentId?.let { parentId ->
                            commentRepository.findByIdAndPostId(parentId, postId) ?: throw IllegalArgumentException("parent not found")
                        }

                    commentRepository.save(
                        Comment.createForPost(
                            content = dto.content,
                            post = post,
                            user = user,
                            parent = parent,
                        ),
                    )

                    entityManager
                        .createQuery("update Post p set p.commentCount = p.commentCount + 1 where p.id = :postId")
                        .setParameter("postId", postId)
                        .executeUpdate()
                }
                return
            } catch (e: Exception) {
                lastError = e
                val deadlock = e.message?.contains("Deadlock found", ignoreCase = true) == true
                if (!deadlock || attempt == maxRetries - 1) {
                    throw e
                }
                Thread.sleep(10)
            }
        }

        throw IllegalStateException("Atomic increment retries exhausted", lastError)
    }
}

@TestConfiguration
class CommentConcurrencyBenchmarkConfig {
    @Bean
    fun atomicCommentCountCommand(
        commentRepository: CommentRepository,
        postRepository: PostRepository,
        userRepository: UserRepository,
        entityManager: EntityManager,
        transactionManager: PlatformTransactionManager,
    ): AtomicCommentCountCommand =
        AtomicCommentCountCommand(
            commentRepository = commentRepository,
            postRepository = postRepository,
            userRepository = userRepository,
            entityManager = entityManager,
            transactionTemplate = TransactionTemplate(transactionManager),
        )
}
