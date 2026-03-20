package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "career-net")
data class CareerNetProperties(
    @field:NotBlank
    val key: String,
    @field:NotBlank
    val baseUrl: String,
)
