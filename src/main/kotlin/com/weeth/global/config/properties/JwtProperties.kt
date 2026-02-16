package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "weeth.jwt")
data class JwtProperties(
    @field:NotBlank
    val key: String,
    val access: TokenProperties,
    val refresh: TokenProperties,
) {
    data class TokenProperties(
        @field:Positive
        val expiration: Long,
        @field:NotBlank
        val header: String,
    )
}
