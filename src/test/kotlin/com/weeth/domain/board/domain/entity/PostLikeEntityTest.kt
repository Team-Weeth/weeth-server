package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.fixture.PostLikeTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostLikeEntityTest :
    StringSpec({
        "초기 생성 시 isActive는 true이다" {
            val like = PostLikeTestFixture.createActive()

            like.isActive shouldBe true
            like.deletedAt shouldBe null
        }

        "activate는 isActive를 true로 설정한다" {
            val like = PostLikeTestFixture.createInactive()

            like.activate()

            like.isActive shouldBe true
        }

        "deactivate는 isActive를 false로 설정한다" {
            val like = PostLikeTestFixture.createActive()

            like.deactivate()

            like.isActive shouldBe false
            like.deletedAt shouldBe null
        }

        "markDeleted는 isActive를 false로 변경하고 deletedAt을 설정한다" {
            val like = PostLikeTestFixture.createActive()
            val now = LocalDateTime.of(2026, 5, 19, 12, 0)

            like.markDeleted(now)

            like.isActive shouldBe false
            like.deletedAt shouldBe now
        }

        "markDeleted는 이미 삭제된 좋아요를 다시 호출해도 deletedAt을 유지한다" {
            val like = PostLikeTestFixture.createActive()
            val deletedAt = LocalDateTime.of(2026, 5, 19, 12, 0)
            like.markDeleted(deletedAt)

            like.markDeleted(deletedAt.plusDays(1))

            like.isActive shouldBe false
            like.deletedAt shouldBe deletedAt
        }
    })
