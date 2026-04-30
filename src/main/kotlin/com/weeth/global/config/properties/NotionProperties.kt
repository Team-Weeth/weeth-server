package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "notion")
data class NotionProperties(
    @field:NotBlank val token: String,
    @field:NotBlank val version: String,
    @field:NotBlank val inquiryDatabaseId: String,
)
