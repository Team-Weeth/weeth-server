package com.weeth.domain.feedback.fixture

import com.weeth.domain.feedback.domain.entity.Feedback
import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.fixture.UserTestFixture

object FeedbackTestFixture {
    fun create(
        user: User = UserTestFixture.createActiveUser1(1L),
        clubId: Long = 1L,
        channel: FeedbackChannel = FeedbackChannel.CHAT,
        content: String = "피드백 내용입니다.",
    ): Feedback =
        Feedback.create(
            user = user,
            clubId = clubId,
            channel = channel,
            content = content,
        )
}
