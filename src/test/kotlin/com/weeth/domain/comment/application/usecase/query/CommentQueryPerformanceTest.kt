package com.weeth.domain.comment.application.usecase.query

import com.weeth.config.QueryCountUtil
import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.domain.entity.enums.Part
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.util.UUID

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("performance")
class CommentQueryPerformanceTest(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val fileRepository: FileRepository,
    private val entityManager: EntityManager,
) : DescribeSpec({
        val runPerformanceTests = System.getProperty("runPerformanceTests")?.toBoolean() ?: false

        fun setupData(
            rootCount: Int,
            childrenPerRoot: Int,
            filesPerComment: Int,
        ): List<Long> {
            val user =
                userRepository.save(
                    User
                        .builder()
                        .name("perf-user")
                        .email("perf-user@test.com")
                        .status(Status.ACTIVE)
                        .position(Position.BE)
                        .role(Role.USER)
                        .build(),
                )
            val post =
                postRepository.save(
                    Post
                        .builder()
                        .user(user)
                        .title("query-performance")
                        .content("measure comment query performance")
                        .category(Category.StudyLog)
                        .part(Part.BE)
                        .parts(listOf(Part.BE))
                        .cardinalNumber(4)
                        .week(1)
                        .comments(ArrayList())
                        .commentCount(0)
                        .build(),
                )

            val commentIds = mutableListOf<Long>()
            repeat(rootCount) { rootIdx ->
                val root =
                    commentRepository.save(
                        Comment.createForPost(
                            content = "root-$rootIdx",
                            post = post,
                            user = user,
                            parent = null,
                        ),
                    )
                commentIds += root.id
                repeat(childrenPerRoot) { childIdx ->
                    val child =
                        commentRepository.save(
                            Comment.createForPost(
                                content = "child-$rootIdx-$childIdx",
                                post = post,
                                user = user,
                                parent = root,
                            ),
                        )
                    commentIds += child.id
                }
            }

            commentIds.forEach { commentId ->
                repeat(filesPerComment) { fileIdx ->
                    fileRepository.save(
                        File.createUploaded(
                            fileName = "file-$commentId-$fileIdx.png",
                            storageKey = "COMMENT/2026-02/${UUID.randomUUID()}_file-$commentId-$fileIdx.png",
                            fileSize = 1024L,
                            contentType = "image/png",
                            ownerType = FileOwnerType.COMMENT,
                            ownerId = commentId,
                        ),
                    )
                }
            }

            return commentIds
        }

        describe("comment file query performance") {
            fun runComparison(
                label: String,
                rootCount: Int,
                childrenPerRoot: Int,
                filesPerComment: Int,
            ) {
                setupData(rootCount = rootCount, childrenPerRoot = childrenPerRoot, filesPerComment = filesPerComment)

                val fileMapper =
                    FileMapper(
                        object : FileAccessUrlPort {
                            override fun resolve(storageKey: String): String = "https://test.local/$storageKey"
                        },
                    )
                val commentMapper = CommentMapper()
                val legacyService = LegacyCommentQueryService(fileRepository, fileMapper, commentMapper)
                val improvedService = GetCommentQueryService(fileRepository, fileMapper, commentMapper)

                entityManager.flush()
                entityManager.clear()

                val legacy =
                    QueryCountUtil.count(entityManager) {
                        val comments = commentRepository.findAll().sortedBy { it.id }
                        val tree = legacyService.toCommentTreeResponses(comments)
                        tree.size shouldBe rootCount
                    }

                entityManager.clear()

                val improved =
                    QueryCountUtil.count(entityManager) {
                        val comments = commentRepository.findAll().sortedBy { it.id }
                        val tree = improvedService.toCommentTreeResponses(comments)
                        tree.size shouldBe rootCount
                    }

                improved.queryCount shouldBeLessThan legacy.queryCount
                println("[$label] LEGACY: $legacy")
                println("[$label] IMPROVED: $improved")
            }

            it("소규모 데이터에서 배치 조회가 더 효율적이다").config(enabled = runPerformanceTests) {
                runComparison(label = "small", rootCount = 10, childrenPerRoot = 1, filesPerComment = 1)
            }

            it("대량 데이터에서도 배치 조회가 더 효율적이다").config(enabled = runPerformanceTests) {
                runComparison(label = "large", rootCount = 200, childrenPerRoot = 1, filesPerComment = 1)
            }
        }
    })

private class LegacyCommentQueryService(
    private val fileRepository: FileRepository,
    private val fileMapper: FileMapper,
    private val commentMapper: CommentMapper,
) {
    fun toCommentTreeResponses(comments: List<Comment>): List<CommentResponse> {
        if (comments.isEmpty()) {
            return emptyList()
        }

        val childrenByParentId =
            comments
                .filter { it.parent != null }
                .groupBy { requireNotNull(it.parent).id }

        return comments
            .filter { it.parent == null }
            .map { mapToCommentResponse(it, childrenByParentId) }
    }

    private fun mapToCommentResponse(
        comment: Comment,
        childrenByParentId: Map<Long, List<Comment>>,
    ): CommentResponse {
        val children =
            childrenByParentId[comment.id]
                ?.map { mapToCommentResponse(it, childrenByParentId) }
                ?: emptyList()

        val files =
            fileRepository
                .findAll(FileOwnerType.COMMENT, comment.id)
                .map(fileMapper::toFileResponse)
                ?: emptyList()

        return commentMapper.toCommentDto(comment, children, files)
    }
}
