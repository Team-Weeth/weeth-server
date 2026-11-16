package com.weeth.domain.releasenote.domain.entity

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 릴리즈 노트. 동아리와 무관한 플랫폼 전역 콘텐츠다(경로/소유에 clubId 없음).
 * 작성/발행은 DEVELOPER 권한만 가능하며, 공개 API에는 [ReleaseNoteStatus.PUBLISHED] 상태만 노출된다.
 */
@Entity
@Table(
    name = "release_notes",
    indexes = [
        // 공개 목록: status + releasedAt 정렬, 백오피스 목록: status + createdAt 정렬에 사용
        Index(name = "idx_release_note_status_released", columnList = "status, released_at"),
        Index(name = "idx_release_note_status_created", columnList = "status, created_at"),
    ],
)
class ReleaseNote private constructor(
    title: String,
    content: String,
    thumbnailUrl: String?,
    releasedAt: LocalDateTime?,
    authorId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "release_note_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    var title: String = title
        private set

    // @Lob을 쓰면 Hibernate가 CLOB으로 매핑해 공개 목록 검색의 LOWER(content)가 깨진다.
    // LONGTEXT를 columnDefinition으로만 지정해 String(기본 타입)으로 매핑한다.
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content
        private set

    @Column(name = "thumbnail_url", nullable = true, length = 2048)
    var thumbnailUrl: String? = thumbnailUrl
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReleaseNoteStatus = ReleaseNoteStatus.DRAFT
        private set

    @Column(name = "released_at", nullable = true)
    var releasedAt: LocalDateTime? = releasedAt
        private set

    @Column(name = "published_at", nullable = true)
    var publishedAt: LocalDateTime? = null
        private set

    @Column(name = "author_id", nullable = false)
    var authorId: Long = authorId
        private set

    /** 공개 API 노출 가능 여부. */
    fun isPublished(): Boolean = status == ReleaseNoteStatus.PUBLISHED

    /** 발행: [DRAFT]→[PUBLISHED]. 이미 발행된 경우 [publishedAt]을 덮어쓰지 않는다. */
    fun publish(now: LocalDateTime) {
        if (status == ReleaseNoteStatus.PUBLISHED) return
        status = ReleaseNoteStatus.PUBLISHED
        publishedAt = now
    }

    /** 비공개 전환: [PUBLISHED]→[DRAFT]. [publishedAt]을 초기화한다. */
    fun unpublish() {
        status = ReleaseNoteStatus.DRAFT
        publishedAt = null
    }

    /** 상태 변경 요청을 대상 상태에 맞게 위임한다. */
    fun changeStatus(
        target: ReleaseNoteStatus,
        now: LocalDateTime,
    ) {
        when (target) {
            ReleaseNoteStatus.PUBLISHED -> publish(now)
            ReleaseNoteStatus.DRAFT -> unpublish()
        }
    }

    /** 전체 교체 수정(PUT). 발행된 노트도 수정할 수 있으며 발행 상태는 바뀌지 않는다. */
    fun update(
        title: String,
        content: String,
        thumbnailUrl: String?,
        releasedAt: LocalDateTime?,
    ) {
        this.title = validatedTitle(title)
        this.content = validatedContent(content)
        this.thumbnailUrl = thumbnailUrl
        this.releasedAt = releasedAt
    }

    companion object {
        const val MAX_TITLE_LENGTH = 100

        private fun validatedTitle(title: String): String {
            val normalized = title.trim()
            require(normalized.isNotBlank()) { "제목은 공백일 수 없습니다." }
            require(normalized.length <= MAX_TITLE_LENGTH) { "제목은 최대 ${MAX_TITLE_LENGTH}자입니다." }
            return normalized
        }

        private fun validatedContent(content: String): String {
            require(content.isNotBlank()) { "본문은 공백일 수 없습니다." }
            return content
        }

        /**
         * 릴리즈 노트를 생성한다. [status]가 [ReleaseNoteStatus.PUBLISHED]면 [now] 기준으로 즉시 발행한다.
         */
        fun create(
            title: String,
            content: String,
            thumbnailUrl: String?,
            releasedAt: LocalDateTime?,
            authorId: Long,
            status: ReleaseNoteStatus,
            now: LocalDateTime,
        ): ReleaseNote =
            ReleaseNote(
                title = validatedTitle(title),
                content = validatedContent(content),
                thumbnailUrl = thumbnailUrl,
                releasedAt = releasedAt,
                authorId = authorId,
            ).apply {
                if (status == ReleaseNoteStatus.PUBLISHED) publish(now)
            }
    }
}
