package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.ManageNotificationOutboxUseCase
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.vo.NotificationOutboxDispatch
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class NotificationOutboxRelayTest :
    DescribeSpec({
        val management = mockk<ManageNotificationOutboxUseCase>()
        val queue = mockk<NotificationQueuePort>()
        val relay = NotificationOutboxRelay(management, queue)
        val first = NotificationOutboxDispatch(1, 10, "event", "a")
        val second = NotificationOutboxDispatch(2, 20, "event", "b")
        beforeTest {
            clearMocks(management, queue)
            every { management.claimBatch(any()) } returns listOf(first, second)
            justRun { management.completePublish(any(), any()) }
        }
        it("SQS 배치에서 성공한 항목만 게시 완료로 반영한다") {
            every { queue.publish(any()) } returns setOf(10L)
            relay.runOnce()
            verify {
                management.completePublish(first, true)
                management.completePublish(second, false)
            }
        }
    })
