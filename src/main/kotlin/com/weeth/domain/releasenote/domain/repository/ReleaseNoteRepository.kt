package com.weeth.domain.releasenote.domain.repository

import com.weeth.domain.releasenote.domain.entity.ReleaseNote
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReleaseNoteRepository : JpaRepository<ReleaseNote, Long> {
    fun findByIdAndStatus(
        id: Long,
        status: ReleaseNoteStatus,
    ): ReleaseNote?

    /**
     * 공개 목록. 발행 노트만, 업데이트 일자(releasedAt, 없으면 publishedAt) 내림차순.
     * [keyword]가 비어 있으면 전체, 있으면 제목/본문 부분 일치.
     */
    @Query(
        """
        SELECT r FROM ReleaseNote r
        WHERE r.status = :status
          AND (:keyword = ''
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY COALESCE(r.releasedAt, r.publishedAt) DESC, r.id DESC
        """,
    )
    fun findPublic(
        @Param("status") status: ReleaseNoteStatus,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Slice<ReleaseNote>

    /** 백오피스 목록. DRAFT 포함, 생성 시각 내림차순. [status]가 null이면 전체. */
    @Query(
        """
        SELECT r FROM ReleaseNote r
        WHERE (:status IS NULL OR r.status = :status)
        ORDER BY r.createdAt DESC, r.id DESC
        """,
    )
    fun findForAdmin(
        @Param("status") status: ReleaseNoteStatus?,
        pageable: Pageable,
    ): Slice<ReleaseNote>
}
