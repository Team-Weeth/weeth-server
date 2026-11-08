package com.weeth.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.dispatch")
data class NotificationDispatchProperties(
    val enabled: Boolean = false,
    val queueUrl: String = "",
    val dlqUrl: String = "",
    val region: String = "ap-northeast-2",
    val endpoint: String? = null,
    val workerCount: Int = 2,
    val receiveBatchSize: Int = 10,
    val longPollingSeconds: Int = 20,
    val visibilitySeconds: Int = 60,
    val leaseSeconds: Long = 60,
    val sendTimeoutMillis: Long = 20000,
    val heartbeatMillis: Long = 10000,
    val relayBatchSize: Int = 100,
) {
    init {
        require(workerCount in 1..32 && receiveBatchSize in 1..10 && longPollingSeconds in 0..20)
        require(visibilitySeconds in 30..43200 && leaseSeconds >= visibilitySeconds)
        require(sendTimeoutMillis in 1 until visibilitySeconds * 1000L)
        require(heartbeatMillis > 0 && heartbeatMillis * 2 < visibilitySeconds * 1000L)
        require(relayBatchSize in 1..500)
        require(!enabled || (queueUrl.isNotBlank() && dlqUrl.isNotBlank())) { "알림 큐 활성화 시 queue-url과 dlq-url이 필요합니다." }
    }
}
