package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.user.domain.entity.User
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mapstruct.factory.Mappers

class PostMapperTest :
    StringSpec({

        val mapper = Mappers.getMapper(PostMapper::class.java)

        "Post를 PostDTO.SaveResponse로 변환" {
            val testUser =
                User
                    .builder()
                    .id(1L)
                    .name("테스트유저")
                    .email("test@weeth.com")
                    .build()

            val testPost =
                Post
                    .builder()
                    .id(1L)
                    .title("테스트 게시글")
                    .user(testUser)
                    .content("테스트 내용입니다.")
                    .build()

            val response = mapper.toSaveResponse(testPost)

            response.shouldNotBeNull()
            response.id() shouldBe testPost.id
        }
    })
