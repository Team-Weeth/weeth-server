package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "spring.data.redis")
data class RedisProperties(
    @field:NotBlank
    val host: String,
    @field:Positive
    val port: Int,
    val password: String?,
)
