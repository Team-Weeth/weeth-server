package com.weeth.domain.releasenote.fixture

import com.weeth.domain.releasenote.domain.entity.ReleaseNote
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

object ReleaseNoteTestFixture {
    val DEFAULT_NOW: LocalDateTime = LocalDateTime.of(2026, 11, 1, 18, 0)

    fun create(
        id: Long = 0L,
        title: String = "게시판 이미지 표시 개선",
        content: String = "<h3>개선된 기능</h3><p>본문 내용입니다.</p>",
        thumbnailUrl: String? = null,
        releasedAt: LocalDateTime? = LocalDateTime.of(2026, 11, 2, 0, 0),
        authorId: Long = 1L,
        status: ReleaseNoteStatus = ReleaseNoteStatus.DRAFT,
        now: LocalDateTime = DEFAULT_NOW,
    ): ReleaseNote =
        ReleaseNote
            .create(
                title = title,
                content = content,
                thumbnailUrl = thumbnailUrl,
                releasedAt = releasedAt,
                authorId = authorId,
                status = status,
                now = now,
            ).applyId(id)

    fun draft(
        id: Long = 0L,
        title: String = "작성 중 노트",
        content: String = "<p>초안</p>",
    ): ReleaseNote = create(id = id, title = title, content = content, status = ReleaseNoteStatus.DRAFT)

    fun published(
        id: Long = 0L,
        title: String = "발행된 노트",
        content: String = "<p>발행본</p>",
    ): ReleaseNote = create(id = id, title = title, content = content, status = ReleaseNoteStatus.PUBLISHED)

    private fun ReleaseNote.applyId(id: Long): ReleaseNote =
        apply {
            if (id != 0L) ReflectionTestUtils.setField(this, "id", id)
        }
}
