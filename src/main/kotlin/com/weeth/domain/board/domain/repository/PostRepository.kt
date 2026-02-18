package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Post
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = ["user"])
    fun findAllByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Slice<Post>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Post?

    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.board.id = :boardId
          AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
    )
    @EntityGraph(attributePaths = ["user"])
    fun searchByBoardId(
        @Param("boardId") boardId: Long,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Slice<Post>
}
