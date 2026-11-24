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
 *
 * 콜백에서 받은 code와 state를 프론트 리다이렉트에 포워딩한다.
 * 앱(Capacitor)에서는 SFSafariViewController 쿠키 저장소가 WKWebView와 격리되므로,
 * 프론트가 code를 받아 POST /api/v4/users/social/apple을 직접 호출하여
 * 자체 쿠키 저장소에 토큰을 저장할 수 있다.
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
        @RequestParam("code", required = false) code: String?,
        @RequestParam("id_token", required = false) idToken: String?,
        @RequestParam("user", required = false) userJson: String?,
        @RequestParam("state", required = false) state: String?,
        @RequestParam("error", required = false) error: String?,
    ): ResponseEntity<Void> {
        if (error != null || idToken.isNullOrBlank()) {
            return redirect(buildRedirectUri(state) { queryParam("error", error ?: "unknown") })
        }

        return try {
            val response = socialLoginUseCase.socialLoginByAppleCallback(idToken, userJson)

            val redirectUri =
                buildRedirectUri(state) {
                    queryParam("registered", response.registered)
                    queryParam("name", response.name)
                    if (!code.isNullOrBlank()) {
                        queryParam("code", code)
                    }
                }

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
            redirect(buildRedirectUri(state) { queryParam("error", "login_failed") })
        }
    }

    private fun buildRedirectUri(
        state: String? = null,
        block: UriComponentsBuilder.() -> Unit,
    ): String =
        UriComponentsBuilder
            .fromUriString(frontendRedirectUri)
            .apply(block)
            .apply { if (!state.isNullOrBlank()) queryParam("state", state) }
            .toUriString()

    private fun redirect(uri: String): ResponseEntity<Void> =
        ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, uri)
            .build()
}
