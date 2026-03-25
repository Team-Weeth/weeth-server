package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "weeth.cookie")
data class CookieProperties(
    @field:NotBlank
    val accessTokenName: String,
    @field:NotBlank
    val refreshTokenName: String,
    val domain: String = "",
    val path: String = "/",
    val refreshPath: String = "/api/v4/users/social/refresh",
    val sameSite: String = "Lax",
    val secure: Boolean = true,
    val httpOnly: Boolean = true,
)
