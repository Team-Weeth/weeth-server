package com.weeth.global.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "auth.providers")
data class OAuthProperties(
    val kakao: KakaoProperties,
    val apple: AppleProperties,
) {
    data class KakaoProperties(
        @field:NotBlank
        val authorizeUri: String,
        @field:NotBlank
        val clientId: String,
        @field:NotBlank
        val redirectUri: String,
        @field:NotBlank
        val grantType: String,
        @field:NotBlank
        val tokenUri: String,
        @field:NotBlank
        val userInfoUri: String,
    )

    data class AppleProperties(
        @field:NotBlank
        val clientId: String,
        @field:NotBlank
        val teamId: String,
        @field:NotBlank
        val keyId: String,
        @field:NotBlank
        val redirectUri: String,
        @field:NotBlank
        val tokenUri: String,
        @field:NotBlank
        val keysUri: String,
        @field:NotBlank
        val privateKeyPath: String,
    )
}
