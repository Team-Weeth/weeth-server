package com.weeth.domain.comment.application.usecase.query

import com.weeth.config.QueryCountUtil
import com.weeth.config.TestContainersConfig
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
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
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val fileRepository: FileRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val entityManager: EntityManager,
) : DescribeSpec({
        val runPerformanceTests = System.getProperty("runPerformanceTests")?.toBoolean() ?: false

        fun createUser(): User =
            userRepository.save(
                User(
                    name = "perf-user",
                    email = Email.from("perf-user@test.com"),
                    department = "컴퓨터공학과",
                    status = Status.ACTIVE,
                ),
            )

        fun createBoard(): Board {
            val club = clubRepository.save(ClubTestFixture.createClub())
            return boardRepository.save(
                Board(
                    club = club,
                    name = "perf-board",
                    type = BoardType.GENERAL,
                ),
            )
        }

        fun createPost(
            clubMember: ClubMember,
            board: Board,
        ): Post =
            postRepository.save(
                Post(
                    title = "query-performance",
                    content = "measure comment query performance",
                    clubMember = clubMember,
                    board = board,
                    cardinalNumber = 4,
                ),
            )

        data class SetupResult(
            val commentIds: List<Long>,
        )

        fun setupData(
            rootCount: Int,
            childrenPerRoot: Int,
            filesPerComment: Int,
        ): SetupResult {
            val user = createUser()
            val board = createBoard()
            val clubMember: ClubMember = clubMemberRepository.save(ClubMember.create(club = board.club, user = user))
            clubMember.accept()
            val post = createPost(clubMember, board)

            val commentIds = mutableListOf<Long>()
            repeat(rootCount) { rootIdx ->
                val root =
                    commentRepository.save(
                        Comment.createForPost(
                            content = "root-$rootIdx",
                            post = post,
                            clubMember = clubMember,
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
                                clubMember = clubMember,
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

            return SetupResult(commentIds)
        }

        describe("comment file query performance") {
            fun runComparison(
                label: String,
                rootCount: Int,
                childrenPerRoot: Int,
                filesPerComment: Int,
            ) {
                val (commentIds) =
                    setupData(
                        rootCount = rootCount,
                        childrenPerRoot = childrenPerRoot,
                        filesPerComment = filesPerComment,
                    )

                val fileAccessUrlPort =
                    object : FileAccessUrlPort {
                        override fun resolve(storageKey: String): String = "https://test.local/$storageKey"
                    }
                val fileMapper = FileMapper(fileAccessUrlPort)
                val commentMapper = CommentMapper(fileAccessUrlPort)
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

        return commentMapper.toCommentDto(comment, children, files)
    }
}
