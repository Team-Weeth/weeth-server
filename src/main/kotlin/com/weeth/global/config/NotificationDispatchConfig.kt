package com.weeth.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Configuration
@EnableConfigurationProperties(NotificationDispatchProperties::class)
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class NotificationDispatchConfig {
    @Bean
    fun notificationSqsClient(properties: NotificationDispatchProperties): SqsClient {
        val builder =
            SqsClient
                .builder()
                .region(Region.of(properties.region))
                .overrideConfiguration {
                    it
                        .apiCallTimeout(
                            Duration.ofSeconds(30),
                        ).apiCallAttemptTimeout(Duration.ofSeconds(25))
                }
        properties.endpoint?.let { builder.endpointOverride(URI.create(it)) }
        return builder.build()
    }

    @Bean(name = ["notificationWorkerExecutor"], destroyMethod = "shutdown")
    fun notificationWorkerExecutor(properties: NotificationDispatchProperties): ExecutorService =
        Executors.newFixedThreadPool(properties.workerCount, threads("notification-worker-"))

    @Bean(name = ["notificationPollExecutor"], destroyMethod = "shutdownNow")
    fun notificationPollExecutor(): ExecutorService = Executors.newSingleThreadExecutor(threads("notification-poll-"))

    @Bean(name = ["notificationIoExecutor"], destroyMethod = "shutdownNow")
    fun notificationIoExecutor(properties: NotificationDispatchProperties): ExecutorService =
        ThreadPoolExecutor(
            properties.workerCount,
            properties.workerCount,
            0,
            TimeUnit.MILLISECONDS,
            SynchronousQueue(),
            threads("notification-fcm-"),
            ThreadPoolExecutor.AbortPolicy(),
        )

    @Bean(name = ["notificationHeartbeatExecutor"], destroyMethod = "shutdownNow")
    fun notificationHeartbeatExecutor(properties: NotificationDispatchProperties): ScheduledExecutorService =
        ScheduledThreadPoolExecutor(properties.workerCount, threads("notification-heartbeat-"))

    @Bean(name = ["notificationMaintenanceScheduler"])
    fun notificationMaintenanceScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 3
            setThreadNamePrefix("notification-maintenance-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
        }

    private fun threads(prefix: String): ThreadFactory {
        val sequence = AtomicInteger()
        return ThreadFactory { runnable -> Thread(runnable, prefix + sequence.incrementAndGet()) }
    }
}
