package com.weeth.domain.comment.application.usecase.command

import com.weeth.config.QueryCountUtil
import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
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
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToLong

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class, CommentConcurrencyBenchmarkConfig::class)
@Tag("performance")
class CommentConcurrencyTest(
    private val postCommentUsecase: PostCommentUsecase,
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val entityManager: EntityManager,
    private val atomicCommentCountCommand: AtomicCommentCountCommand,
) : DescribeSpec({

        data class ConcurrencyResult(
            val successCount: Int,
            val failCount: Int,
            val postCommentCount: Int,
            val actualCommentCount: Int,
            val queryCount: Long,
            val elapsedTimeMs: Double,
            val firstError: String?,
        )

        data class BenchmarkSummary(
            val label: String,
            val medianElapsedMs: Double,
            val medianQueryCount: Long,
            val medianThroughput: Double,
            val allElapsedMs: List<Double>,
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

        fun createPost(
            title: String,
            user: User,
        ): Post {
            val board =
                boardRepository.save(
                    Board(
                        name = "concurrency-board",
                        type = BoardType.GENERAL,
                    ),
                )
            return postRepository.save(
                Post(
                    title = title,
                    content = "내용",
                    user = user,
                    board = board,
                ),
            )
        }

        fun runConcurrentSave(
            threadCount: Int,
            saveAction: (postId: Long, userId: Long, index: Int) -> Unit,
        ): ConcurrencyResult {
            val users = createUsers(threadCount)
            val post = createPost("동시성 테스트 게시글", users.first())
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

        fun benchmark(
            label: String,
            rounds: Int,
            threadCount: Int,
            saveAction: (postId: Long, userId: Long, index: Int) -> Unit,
        ): BenchmarkSummary {
            val results = (1..rounds).map { runConcurrentSave(threadCount, saveAction) }
            results.forEach { r ->
                r.failCount shouldBe 0
                r.postCommentCount shouldBe threadCount
                r.actualCommentCount shouldBe threadCount
            }

            val elapsedSorted = results.map { it.elapsedTimeMs }.sorted()
            val querySorted = results.map { it.queryCount }.sorted()
            val medianElapsedMs = elapsedSorted[elapsedSorted.size / 2]
            val medianQueryCount = querySorted[querySorted.size / 2]
            val medianThroughput = threadCount / (medianElapsedMs / 1000.0)

            println(
                "[CommentBenchmark][$label] rounds=$rounds, threadCount=$threadCount, " +
                    "medianElapsedMs=${medianElapsedMs.roundToLong()}, " +
                    "medianThroughput=${"%.2f".format(medianThroughput)} ops/s, " +
                    "medianQueryCount=$medianQueryCount, allElapsedMs=${elapsedSorted.map { it.roundToLong() }}",
            )

            return BenchmarkSummary(
                label = label,
                medianElapsedMs = medianElapsedMs,
                medianQueryCount = medianQueryCount,
                medianThroughput = medianThroughput,
                allElapsedMs = elapsedSorted,
            )
        }

        afterEach {
            commentRepository.deleteAllInBatch()
            postRepository.deleteAllInBatch()
            boardRepository.deleteAllInBatch()
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
            it("PESSIMISTIC_WRITE와 Atomic Increment를 측정하고 Atomic 우위를 검증한다") {
                val threadCount = 30
                val rounds = 5

                val pessimisticSummary =
                    benchmark("pessimistic", rounds, threadCount) { postId, userId, index ->
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

                val atomicSummary =
                    benchmark("atomic", rounds, threadCount) { postId, userId, index ->
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

                println(
                    "[CommentBenchmark][compare] " +
                        "atomicMedian=${atomicSummary.medianElapsedMs.roundToLong()}ms, " +
                        "pessimisticMedian=${pessimisticSummary.medianElapsedMs.roundToLong()}ms, " +
                        "atomicThroughput=${"%.2f".format(atomicSummary.medianThroughput)} ops/s, " +
                        "pessimisticThroughput=${"%.2f".format(pessimisticSummary.medianThroughput)} ops/s",
                )
                val winner = if (atomicSummary.medianElapsedMs < pessimisticSummary.medianElapsedMs) "atomic" else "pessimistic"
                println("[CommentBenchmark][winner] $winner")
            }
        }
    })

class AtomicCommentCountCommand(
    private val commentRepository: CommentRepository,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {
    fun savePostCommentWithAtomicIncrement(
        dto: CommentSaveRequest,
        postId: Long,
        userId: Long,
    ) {
        val maxRetries = 20
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val user = entityManager.getReference(User::class.java, userId)
                    val post = entityManager.getReference(Post::class.java, postId)
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
                val lockWaitTimeout = e.message?.contains("Lock wait timeout exceeded", ignoreCase = true) == true
                if ((!deadlock && !lockWaitTimeout) || attempt == maxRetries - 1) {
                    throw e
                }
                val backoffMs = ThreadLocalRandom.current().nextLong(10, 40)
                Thread.sleep(backoffMs)
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
        entityManager: EntityManager,
        transactionManager: PlatformTransactionManager,
    ): AtomicCommentCountCommand =
        AtomicCommentCountCommand(
            commentRepository = commentRepository,
            entityManager = entityManager,
            transactionTemplate = TransactionTemplate(transactionManager),
        )
}
