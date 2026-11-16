package com.weeth.domain.feedback.application.event

import com.weeth.domain.feedback.domain.enums.FeedbackChannel

/**
 * OSIV가 비활성화되어 있어 커밋 이후 지연 로딩이 불가능하므로,
 * 알림에 필요한 동아리명·작성자명을 트랜잭션 안에서 문자열로 미리 담아 전달한다.
 */
data class FeedbackCreatedEvent(
    val clubName: String,
    val authorName: String,
    val channel: FeedbackChannel,
    val content: String,
)
