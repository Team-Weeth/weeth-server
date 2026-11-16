package com.weeth.domain.releasenote.domain.entity

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ReleaseNoteTest :
    StringSpec({
        val now = LocalDateTime.of(2026, 11, 1, 18, 0)

        fun create(
            title: String = "제목",
            content: String = "<p>본문</p>",
            status: ReleaseNoteStatus = ReleaseNoteStatus.DRAFT,
        ) = ReleaseNote.create(
            title = title,
            content = content,
            thumbnailUrl = null,
            releasedAt = null,
            authorId = 1L,
            status = status,
            now = now,
        )

        "create는 기본적으로 DRAFT 상태이며 publishedAt이 없다" {
            val note = create(status = ReleaseNoteStatus.DRAFT)

            note.status shouldBe ReleaseNoteStatus.DRAFT
            note.publishedAt.shouldBeNull()
        }

        "create에 PUBLISHED를 주면 즉시 발행되고 publishedAt이 now로 기록된다" {
            val note = create(status = ReleaseNoteStatus.PUBLISHED)

            note.status shouldBe ReleaseNoteStatus.PUBLISHED
            note.publishedAt shouldBe now
        }

        "create는 제목 앞뒤 공백을 제거한다" {
            create(title = "  제목  ").title shouldBe "제목"
        }

        "create는 제목이 공백뿐이면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> { create(title = "   ") }
        }

        "create는 제목이 100자를 초과하면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> { create(title = "가".repeat(ReleaseNote.MAX_TITLE_LENGTH + 1)) }
        }

        "create는 본문이 공백뿐이면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> { create(content = "   ") }
        }

        "publish는 DRAFT를 PUBLISHED로 바꾸고 publishedAt을 기록한다" {
            val note = create(status = ReleaseNoteStatus.DRAFT)
            val publishedAt = LocalDateTime.of(2026, 11, 3, 9, 0)

            note.publish(publishedAt)

            note.status shouldBe ReleaseNoteStatus.PUBLISHED
            note.publishedAt shouldBe publishedAt
        }

        "publish는 이미 발행된 노트의 publishedAt을 덮어쓰지 않는다" {
            val note = create(status = ReleaseNoteStatus.PUBLISHED)

            note.publish(LocalDateTime.of(2027, 1, 1, 0, 0))

            note.publishedAt shouldBe now
        }

        "unpublish는 PUBLISHED를 DRAFT로 바꾸고 publishedAt을 초기화한다" {
            val note = create(status = ReleaseNoteStatus.PUBLISHED)

            note.unpublish()

            note.status shouldBe ReleaseNoteStatus.DRAFT
            note.publishedAt.shouldBeNull()
        }

        "changeStatus(PUBLISHED)는 발행, changeStatus(DRAFT)는 비공개 전환이다" {
            val note = create(status = ReleaseNoteStatus.DRAFT)
            val at = LocalDateTime.of(2026, 11, 5, 12, 0)

            note.changeStatus(ReleaseNoteStatus.PUBLISHED, at)
            note.status shouldBe ReleaseNoteStatus.PUBLISHED
            note.publishedAt shouldBe at

            note.changeStatus(ReleaseNoteStatus.DRAFT, at)
            note.status shouldBe ReleaseNoteStatus.DRAFT
            note.publishedAt.shouldBeNull()
        }

        "update는 제목/본문/썸네일/반영일을 교체하되 발행 상태는 유지한다" {
            val note = create(status = ReleaseNoteStatus.PUBLISHED)
            val releasedAt = LocalDateTime.of(2026, 12, 1, 0, 0)

            note.update(title = "새 제목", content = "<p>새 본문</p>", thumbnailUrl = "https://img", releasedAt = releasedAt)

            note.title shouldBe "새 제목"
            note.content shouldBe "<p>새 본문</p>"
            note.thumbnailUrl shouldBe "https://img"
            note.releasedAt shouldBe releasedAt
            note.status shouldBe ReleaseNoteStatus.PUBLISHED
            note.publishedAt shouldBe now
        }

        "update는 제목이 공백뿐이면 예외를 던진다" {
            val note = create()
            shouldThrow<IllegalArgumentException> {
                note.update(title = "  ", content = "<p>x</p>", thumbnailUrl = null, releasedAt = null)
            }
        }
    })
