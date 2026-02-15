package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.user.domain.entity.User

object NoticeTestFixture {
    fun createNotice(
        id: Long? = null,
        title: String,
        user: User? = null,
    ): Notice =
        Notice
            .builder()
            .id(id)
            .title(title)
            .content("내용")
            .user(user)
            .comments(ArrayList())
            .commentCount(0)
            .build()
}
