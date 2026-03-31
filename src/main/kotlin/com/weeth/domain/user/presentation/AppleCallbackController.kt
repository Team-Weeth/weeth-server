package com.weeth.domain.user.presentation

import com.fasterxml.jackson.databind.ObjectMapper
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
 * Apple 인가 요청 예시:
 * https://appleid.apple.com/auth/authorize
 * ?response_type=code id_token
 * &response_mode=form_post
 * &client_id=com.weeth.web
 * &redirect_uri={서버 콜백 URL}
 * &scope=name email
 *
 * Apple이 redirect_uri로 POST (application/x-www-form-urlencoded) 요청을 보내며,
 * id_token을 직접 검증(code 교환 불필요)하고 user JSON에서 이름을 추출한 뒤
 * 프론트엔드로 리다이렉트한다.
 */
@Hidden
@RestController
class AppleCallbackController(
    private val socialLoginUseCase: SocialLoginUseCase,
    private val objectMapper: ObjectMapper,
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
        @RequestParam("error", required = false) error: String?,
    ): ResponseEntity<Void> {
        if (error != null || idToken.isNullOrBlank()) {
            log.warn("Apple 콜백 오류: error={}", error)
            return redirect(
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("error", error ?: "unknown")
                    .toUriString(),
            )
        }

        return try {
            val userName = parseAppleUserName(userJson)
            val response = socialLoginUseCase.socialLoginByAppleCallback(idToken, userName)

            val redirectUri =
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("registered", response.registered)
                    .queryParam("name", response.name)
                    .toUriString()

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
            redirect(
                UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("error", "login_failed")
                    .toUriString(),
            )
        }
    }

    private fun redirect(uri: String): ResponseEntity<Void> =
        ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, uri)
            .build()

    private fun parseAppleUserName(userJson: String?): String? {
        if (userJson.isNullOrBlank()) return null
        return try {
            val node = objectMapper.readTree(userJson)
            val nameNode = node["name"] ?: return null
            val firstName = nameNode["firstName"]?.asText()?.trim() ?: ""
            val lastName = nameNode["lastName"]?.asText()?.trim() ?: ""
            "$lastName$firstName".trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            log.warn("Apple user JSON 파싱 실패: {}", e.message)
            null
        }
    }
}
