package com.weeth.domain.comment.domain.entity

import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.comment.fixture.CommentTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentEntityTest :
    DescribeSpec({
        val user = UserTestFixture.createActiveUser1(1L)
        val post = PostTestFixture.create(id = 10L, title = "title")

        describe("createForPost") {
            it("부모 없이 최상위 댓글을 생성한다") {
                val comment = Comment.createForPost(content = "내용", post = post, user = user, parent = null)

                comment.content shouldBe "내용"
                comment.post shouldBe post
                comment.user shouldBe user
                comment.parent shouldBe null
            }

            it("부모 댓글이 다른 게시글이면 예외를 던진다") {
                val otherPost = PostTestFixture.create(id = 99L, title = "other")
                val parent = CommentTestFixture.createPostComment(id = 100L, post = otherPost, user = user)

                shouldThrow<IllegalArgumentException> {
                    Comment.createForPost(content = "대댓글", post = post, user = user, parent = parent)
                }
            }
        }

        describe("markAsDeleted") {
            it("isDeleted를 true로 바꾸고 내용을 대체 문구로 변경한다") {
                val comment = CommentTestFixture.createPostComment(post = post, user = user)

                comment.markAsDeleted()

                comment.isDeleted shouldBe true
                comment.content shouldBe "삭제된 댓글입니다."
            }
        }
    })
