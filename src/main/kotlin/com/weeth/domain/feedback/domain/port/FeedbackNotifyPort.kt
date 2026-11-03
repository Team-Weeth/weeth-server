package com.weeth.domain.feedback.domain.port

import com.weeth.domain.feedback.domain.enums.FeedbackChannel

/**
 * 서비스 피드백이 저장된 뒤 운영 채널(Slack 등)로 알림을 보내기 위한 포트.
 */
interface FeedbackNotifyPort {
    fun notify(
        clubName: String,
        authorName: String,
        channel: FeedbackChannel,
        content: String,
    )
}
