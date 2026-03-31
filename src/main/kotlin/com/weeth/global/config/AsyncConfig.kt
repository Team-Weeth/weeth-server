package com.weeth.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncConfig {
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 5
            maxPoolSize = 10
            queueCapacity = 50
            setThreadNamePrefix("async-")
            initialize()
        }
}
