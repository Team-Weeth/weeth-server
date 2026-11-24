package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.global.auth.jwt.application.service.TokenCookieProvider
import com.weeth.global.config.properties.OAuthProperties
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

/**
 * Apple Sign in with Apple의 form_post 콜백을 처리하는 컨트롤러.
 */
@Hidden
@RestController
class SocialCallbackController(
    private val socialLoginUseCase: SocialLoginUseCase,
    private val tokenCookieProvider: TokenCookieProvider,
    oAuthProperties: OAuthProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val frontendRedirectUri = oAuthProperties.apple.frontendRedirectUri

    @PostMapping(
        "/api/v4/users/social/apple/callback",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun handleCallback(
        @RequestParam("id_token", required = false) idToken: String?,
        @RequestParam("user", required = false) userJson: String?,
        @RequestParam("state", required = false) state: String?,
        @RequestParam("error", required = false) error: String?,
    ): ResponseEntity<Void> {
        if (error != null || idToken.isNullOrBlank()) {
            val errorBuilder =
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("error", error ?: "unknown")
            if (!state.isNullOrBlank()) {
                errorBuilder.queryParam("state", state)
            }
            return redirect(errorBuilder.toUriString())
        }

        return try {
            val response = socialLoginUseCase.socialLoginByAppleCallback(idToken, userJson)

            val redirectBuilder =
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("registered", response.registered)
                    .queryParam("name", response.name)
            if (!state.isNullOrBlank()) {
                redirectBuilder.queryParam("state", state)
            }
            val redirectUri = redirectBuilder.toUriString()

            ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri)
                .header(
                    HttpHeaders.SET_COOKIE,
                    tokenCookieProvider.createAccessTokenCookie(response.accessToken).toString(),
                ).header(
                    HttpHeaders.SET_COOKIE,
                    tokenCookieProvider.createRefreshTokenCookie(response.refreshToken).toString(),
                ).build()
        } catch (e: Exception) {
            log.error("Apple 콜백 처리 중 오류 발생", e)
            val failBuilder =
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("error", "login_failed")
            if (!state.isNullOrBlank()) {
                failBuilder.queryParam("state", state)
            }
            redirect(failBuilder.toUriString())
        }
    }

    private fun redirect(uri: String): ResponseEntity<Void> =
        ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, uri)
            .build()
}
