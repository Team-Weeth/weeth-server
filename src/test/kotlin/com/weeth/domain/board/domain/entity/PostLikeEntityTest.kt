package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.fixture.PostLikeTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PostLikeEntityTest :
    StringSpec({
        "초기 생성 시 isActive는 true이다" {
            val like = PostLikeTestFixture.createActive()

            like.isActive shouldBe true
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
        }
    })
