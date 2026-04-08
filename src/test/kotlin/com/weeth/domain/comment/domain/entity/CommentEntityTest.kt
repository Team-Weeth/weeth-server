package com.weeth.domain.comment.domain.entity

import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.comment.fixture.CommentTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentEntityTest :
    DescribeSpec({
        val member = ClubMemberTestFixture.createActiveMember()
        val post = PostTestFixture.create(title = "title", clubMember = member)

        describe("createForPost") {
            it("부모 없이 최상위 댓글을 생성한다") {
                val comment = Comment.createForPost(content = "내용", post = post, clubMember = member, parent = null)

                comment.content shouldBe "내용"
                comment.post shouldBe post
                comment.clubMember shouldBe member
                comment.parent shouldBe null
            }
        }

        describe("markAsDeleted") {
            it("isDeleted를 true로 바꾸고 내용을 대체 문구로 변경한다") {
                val comment = CommentTestFixture.createPostComment(post = post, clubMember = member)

                comment.markAsDeleted()

                comment.isDeleted shouldBe true
                comment.content shouldBe "삭제된 댓글입니다."
            }
        }
    })
