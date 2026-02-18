package com.weeth.domain.comment.domain.entity

import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.fixture.NoticeTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.comment.fixture.CommentTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentEntityTest :
    DescribeSpec({
        val user = UserTestFixture.createActiveUser1(1L)
        val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
        val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = user)

        describe("createForPost") {
            it("부모 없이 최상위 댓글을 생성한다") {
                val comment = Comment.createForPost(content = "내용", post = post, user = user, parent = null)

                comment.content shouldBe "내용"
                comment.post shouldBe post
                comment.user shouldBe user
                comment.parent shouldBe null
            }

            it("부모 댓글이 같은 게시글이면 대댓글로 생성된다") {
                val parent = CommentTestFixture.createPostComment(id = 100L, post = post, user = user)
                val child = Comment.createForPost(content = "대댓글", post = post, user = user, parent = parent)

                child.parent shouldBe parent
            }

            it("부모 댓글이 다른 게시글이면 예외를 던진다") {
                val otherPost = PostTestFixture.createPost(id = 99L, title = "other", category = Category.StudyLog)
                val parent = CommentTestFixture.createPostComment(id = 100L, post = otherPost, user = user)

                shouldThrow<IllegalArgumentException> {
                    Comment.createForPost(content = "대댓글", post = post, user = user, parent = parent)
                }
            }
        }

        describe("createForNotice") {
            it("부모 없이 최상위 댓글을 생성한다") {
                val comment = Comment.createForNotice(content = "내용", notice = notice, user = user, parent = null)

                comment.content shouldBe "내용"
                comment.notice shouldBe notice
                comment.parent shouldBe null
            }

            it("부모 댓글이 다른 공지글이면 예외를 던진다") {
                val otherNotice = NoticeTestFixture.createNotice(id = 99L, title = "other", user = user)
                val parent = CommentTestFixture.createNoticeComment(id = 100L, notice = otherNotice, user = user)

                shouldThrow<IllegalArgumentException> {
                    Comment.createForNotice(content = "대댓글", notice = notice, user = user, parent = parent)
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

        describe("updateContent") {
            it("내용을 새 값으로 변경한다") {
                val comment = CommentTestFixture.createPostComment(content = "원래 내용", post = post, user = user)

                comment.updateContent("수정된 내용")

                comment.content shouldBe "수정된 내용"
            }

            it("빈 문자열이면 예외를 던진다") {
                val comment = CommentTestFixture.createPostComment(post = post, user = user)

                shouldThrow<IllegalArgumentException> {
                    comment.updateContent("")
                }
            }

            it("300자를 초과하면 예외를 던진다") {
                val comment = CommentTestFixture.createPostComment(post = post, user = user)

                shouldThrow<IllegalArgumentException> {
                    comment.updateContent("a".repeat(301))
                }
            }
        }

        describe("isOwnedBy") {
            it("작성자 ID가 일치하면 true를 반환한다") {
                val comment = CommentTestFixture.createPostComment(post = post, user = user)

                comment.isOwnedBy(1L) shouldBe true
            }

            it("작성자 ID가 다르면 false를 반환한다") {
                val comment = CommentTestFixture.createPostComment(post = post, user = user)

                comment.isOwnedBy(99L) shouldBe false
            }
        }
    })
