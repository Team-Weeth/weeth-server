package com.weeth.domain.feedback.application.event

import com.weeth.domain.feedback.domain.port.FeedbackNotifyPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SendFeedbackNotifyEventListener(
    private val feedbackNotifyPort: FeedbackNotifyPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: FeedbackCreatedEvent) {
        try {
            feedbackNotifyPort.notify(
                clubName = event.clubName,
                authorName = event.authorName,
                channel = event.channel,
                content = event.content,
            )
        } catch (exception: Exception) {
            log.warn(
                "서비스 피드백 Slack 알림 전송 실패. clubName={}, channel={}",
                event.clubName,
                event.channel,
                exception,
            )
        }
    }
}
