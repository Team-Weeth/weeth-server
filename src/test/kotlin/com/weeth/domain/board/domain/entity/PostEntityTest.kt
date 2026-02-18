package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.fixture.PostTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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
                newCardinalNumber = 7,
            )

            post.title shouldBe "변경"
            post.cardinalNumber shouldBe 7
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

    })
