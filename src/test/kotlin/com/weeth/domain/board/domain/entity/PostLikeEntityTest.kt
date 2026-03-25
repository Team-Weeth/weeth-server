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

        "toggle은 isActive를 true에서 false로 반전시킨다" {
            val like = PostLikeTestFixture.createActive()

            like.toggle()

            like.isActive shouldBe false
        }

        "toggle을 두 번 호출하면 isActive가 다시 true가 된다" {
            val like = PostLikeTestFixture.createActive()

            like.toggle()
            like.toggle()

            like.isActive shouldBe true
        }
    })
