package com.weeth.global.auth.kakao

import com.weeth.global.auth.kakao.dto.KakaoTokenResponse
import com.weeth.global.auth.kakao.dto.KakaoUserInfoResponse
import com.weeth.global.config.properties.OAuthProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class KakaoAuthService(
    oAuthProperties: OAuthProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val kakaoProperties = oAuthProperties.kakao
    private val restClient = restClientBuilder.build()

    fun getKakaoToken(authCode: String): KakaoTokenResponse {
        val body =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", kakaoProperties.grantType)
                add("client_id", kakaoProperties.clientId)
                add("redirect_uri", kakaoProperties.redirectUri)
                add("code", authCode)
            }

        return requireNotNull(
            restClient
                .post()
                .uri(kakaoProperties.tokenUri)
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body<KakaoTokenResponse>(),
        )
    }

    fun getUserInfo(accessToken: String): KakaoUserInfoResponse =
        requireNotNull(
            restClient
                .get()
                .uri(kakaoProperties.userInfoUri)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body<KakaoUserInfoResponse>(),
        )
}
