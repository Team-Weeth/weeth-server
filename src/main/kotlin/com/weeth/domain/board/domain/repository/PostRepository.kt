package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.enums.BoardType
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PostRepository :
    JpaRepository<Post, Long>,
    PostReader {
    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile", "board"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.id IN :boardIds
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    fun findAllActiveByBoardIds(
        @Param("boardIds") boardIds: List<Long>,
        pageable: Pageable,
    ): Slice<Post>

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile", "board"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.id = :boardId
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        """,
    )
    fun findAllActiveByBoardId(
        @Param("boardId") boardId: Long,
        pageable: Pageable,
    ): Slice<Post>

    fun findByIdAndIsDeletedFalse(id: Long): Post?

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile", "board"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.id = :id
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        """,
    )
    fun findActivePostById(
        @Param("id") id: Long,
    ): Post?

    @EntityGraph(attributePaths = ["board", "board.club"])
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.id = :id
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        """,
    )
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Post?

    @EntityGraph(attributePaths = ["board", "board.club"])
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.id IN :ids
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.id ASC
        """,
    )
    fun findAllByIdsWithLock(
        @Param("ids") ids: List<Long>,
    ): List<Post>

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile", "board"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.id = :boardId
          AND p.isDeleted = false
          AND p.board.isDeleted = false
          AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
    )
    fun searchByBoardId(
        @Param("boardId") boardId: Long,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Slice<Post>

    override fun getById(postId: Long): Post = findActivePostById(postId) ?: throw PostNotFoundException()

    override fun findActiveById(postId: Long): Post? = findActivePostById(postId)

    override fun findRecentByBoardIds(
        boardIds: List<Long>,
        pageable: Pageable,
    ): Slice<Post> = findAllActiveByBoardIds(boardIds, pageable)

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.type = :boardType
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    override fun findRecentByBoardType(
        @Param("boardType") boardType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.type <> :excludedType
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    override fun findRecentExcludingBoardType(
        @Param("excludedType") excludedType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.club.id = :clubId
          AND p.board.type = :boardType
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    override fun findRecentByClubIdAndBoardType(
        @Param("clubId") clubId: Long,
        @Param("boardType") boardType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Query(
        """
        SELECT p
        FROM Post p
        LEFT JOIN LastNoticeRead lr ON lr.clubMember.id = :clubMemberId AND lr.board.id = p.board.id
        WHERE p.board.club.id = :clubId
          AND p.board.type = :boardType
          AND p.isDeleted = false
          AND p.board.isDeleted = false
          AND p.createdAt >= :since
          AND (lr IS NULL OR p.createdAt > lr.lastReadAt)
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    fun findUnreadNoticeSince(
        @Param("clubId") clubId: Long,
        @Param("clubMemberId") clubMemberId: Long,
        @Param("boardType") boardType: BoardType,
        @Param("since") since: LocalDateTime,
        pageable: Pageable,
    ): List<Post>

    override fun findFirstUnreadNoticeSince(
        clubId: Long,
        clubMemberId: Long,
        boardType: BoardType,
        since: LocalDateTime,
    ): Post? = findUnreadNoticeSince(clubId, clubMemberId, boardType, since, PageRequest.of(0, 1)).firstOrNull()

    @Query(
        """
        SELECT new com.weeth.domain.board.domain.repository.BoardPostCount(p.board.id, COUNT(p))
        FROM Post p
        WHERE p.board.id IN :boardIds
          AND p.isDeleted = false
        GROUP BY p.board.id
        """,
    )
    fun countActivePostsByBoardIds(
        @Param("boardIds") boardIds: List<Long>,
    ): List<BoardPostCount>

    @Query(
        """
        SELECT p.id
        FROM Post p
        WHERE p.clubMember.id IN :clubMemberIds
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.id ASC
        """,
    )
    fun findActiveIdsByClubMemberIdIn(
        @Param("clubMemberIds") clubMemberIds: List<Long>,
    ): List<Long>

    @Query(
        """
        SELECT COUNT(p)
        FROM Post p
        WHERE p.clubMember.id IN :clubMemberIds
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        """,
    )
    override fun countActiveByClubMemberIds(
        @Param("clubMemberIds") clubMemberIds: List<Long>,
    ): Long

    @EntityGraph(attributePaths = ["board", "board.club"])
    @Query(
        value = """
        SELECT p
        FROM Post p
        WHERE p.clubMember.user.id = :userId
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        ORDER BY p.createdAt DESC, p.id DESC
        """,
        countQuery = """
        SELECT COUNT(p)
        FROM Post p
        WHERE p.clubMember.user.id = :userId
          AND p.isDeleted = false
          AND p.board.isDeleted = false
        """,
    )
    override fun findMyActivePosts(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<Post>
}
