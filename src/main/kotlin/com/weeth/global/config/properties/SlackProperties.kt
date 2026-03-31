package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "slack")
data class SlackProperties(
    @field:NotBlank val webhookUrl: String,
)
