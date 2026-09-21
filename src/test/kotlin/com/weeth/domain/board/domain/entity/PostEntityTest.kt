package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class PostEntityTest :
    StringSpec({
        "increaseCommentCount는 댓글 수를 1 증가시킨다" {
            val post = PostTestFixture.create()

            post.increaseCommentCount()

            post.commentCount shouldBe 1
        }

        "decreaseCommentCount는 0이면 예외를 던진다" {
            val post = PostTestFixture.create()

            shouldThrow<IllegalStateException> {
                post.decreaseCommentCount()
            }
        }

        "update는 게시글 필드를 갱신한다" {
            val post = PostTestFixture.create()

            post.update(
                newTitle = "변경",
                newContent = "변경 내용",
            )

            post.title shouldBe "변경"
            post.content shouldBe "변경 내용"
        }

        "update는 content가 공백이면 예외를 던진다" {
            val post = PostTestFixture.create()

            shouldThrow<IllegalArgumentException> {
                post.update(
                    newTitle = "변경",
                    newContent = "   ",
                )
            }
        }

        "increaseLikeCount는 좋아요 수를 1 증가시킨다" {
            val post = PostTestFixture.create()

            post.increaseLikeCount()

            post.likeCount shouldBe 1
        }

        "decreaseLikeCount는 0이면 예외를 던진다" {
            val post = PostTestFixture.create()

            shouldThrow<IllegalStateException> {
                post.decreaseLikeCount()
            }
        }

        "markDeleted와 restore는 삭제 상태를 토글한다" {
            val post = PostTestFixture.create()

            post.markDeleted()
            post.isDeleted shouldBe true

            post.restore()
            post.isDeleted shouldBe false
        }

        "hasLegacyMarkdownContent는 이관 동아리의 전환 이전 글에만 true를 반환한다" {
            val legacyClubId = 100L
            val editorMigratedAt = LocalDateTime.of(2026, 9, 20, 0, 0)

            fun postOf(
                clubId: Long,
                createdAt: LocalDateTime,
            ): Post {
                val club = ClubTestFixture.createClub(id = clubId)
                val post = PostTestFixture.create(board = BoardTestFixture.create(club = club))
                ReflectionTestUtils.setField(post, "createdAt", createdAt)
                return post
            }

            postOf(legacyClubId, editorMigratedAt.minusDays(1))
                .hasLegacyMarkdownContent(legacyClubId, editorMigratedAt) shouldBe true

            // 다른 동아리 글은 v4 에서 작성된 것이므로 변환 대상이 아니다
            postOf(999L, editorMigratedAt.minusDays(1))
                .hasLegacyMarkdownContent(legacyClubId, editorMigratedAt) shouldBe false

            postOf(legacyClubId, editorMigratedAt.plusSeconds(1))
                .hasLegacyMarkdownContent(legacyClubId, editorMigratedAt) shouldBe false

            // 전환 시각 정각부터는 v4 에디터로 작성된 글이다
            postOf(legacyClubId, editorMigratedAt)
                .hasLegacyMarkdownContent(legacyClubId, editorMigratedAt) shouldBe false
        }
    })
