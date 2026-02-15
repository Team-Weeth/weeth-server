package com.weeth.domain.board.fixture

import com.weeth.domain.board.application.dto.PostDTO
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.domain.entity.enums.Part
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import java.time.LocalDateTime

object PostTestFixture {
    fun createPost(
        id: Long,
        title: String,
        category: Category,
    ): Post =
        Post
            .builder()
            .id(id)
            .title(title)
            .content("내용")
            .comments(ArrayList())
            .commentCount(0)
            .category(category)
            .build()

    fun createEducationPost(
        id: Long,
        user: User,
        title: String,
        category: Category,
        parts: List<Part>,
        cardinalNumber: Int,
        week: Int,
    ): Post =
        Post
            .builder()
            .id(id)
            .user(user)
            .title(title)
            .content("내용")
            .parts(parts)
            .cardinalNumber(cardinalNumber)
            .week(week)
            .commentCount(0)
            .category(Category.Education)
            .comments(ArrayList())
            .build()

    fun createResponseAll(post: Post): PostDTO.ResponseAll =
        PostDTO.ResponseAll
            .builder()
            .id(post.id)
            .part(post.part)
            .role(Role.USER)
            .title(post.title)
            .content(post.content)
            .studyName(post.studyName)
            .week(post.week)
            .time(LocalDateTime.now())
            .commentCount(post.commentCount)
            .hasFile(false)
            .isNew(false)
            .build()

    fun createResponseEducationAll(
        post: Post,
        fileExists: Boolean,
    ): PostDTO.ResponseEducationAll =
        PostDTO.ResponseEducationAll
            .builder()
            .id(post.id)
            .name(post.user.name)
            .parts(post.parts)
            .position(post.user.position)
            .role(post.user.role)
            .title(post.title)
            .content(post.content)
            .time(post.createdAt)
            .commentCount(post.commentCount)
            .hasFile(fileExists)
            .isNew(false)
            .build()
}
