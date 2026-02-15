package com.weeth.global.auth.kakao;

import com.weeth.global.auth.kakao.dto.KakaoTokenResponse;
import com.weeth.global.auth.kakao.dto.KakaoUserInfoResponse;
import com.weeth.global.config.properties.OAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class KakaoAuthService {

    private final OAuthProperties.KakaoProperties kakaoProperties;
    private final RestClient restClient = RestClient.create();

    public KakaoAuthService(OAuthProperties oAuthProperties) {
        this.kakaoProperties = oAuthProperties.getKakao();
    }

    public KakaoTokenResponse getKakaoToken(String authCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", kakaoProperties.getGrantType());
        body.add("client_id", kakaoProperties.getClientId());
        body.add("redirect_uri", kakaoProperties.getRedirectUri());
        body.add("code", authCode);

        return restClient.post()
                .uri(kakaoProperties.getTokenUri())
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(KakaoTokenResponse.class);
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        return restClient.get()
                .uri(kakaoProperties.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);

    }
}
