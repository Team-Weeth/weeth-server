package leets.weeth.global.auth.kakao;

import leets.weeth.global.auth.kakao.dto.KakaoTokenResponse;
import leets.weeth.global.auth.kakao.dto.KakaoUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class KakaoAuthService {

    @Value("${auth.providers.kakao.client_id}")
    private String kakaoClientId;
    @Value("${auth.providers.kakao.redirect_uri}")
    private String redirectUri;
    @Value("${auth.providers.kakao.grant_type}")
    private String grantType;
    @Value("${auth.providers.kakao.token_uri}")
    private String tokenUri;
    @Value("${auth.providers.kakao.user_info_uri}")
    private String userInfoUri;

    private final RestClient restClient = RestClient.create();

    public KakaoTokenResponse getKakaoToken(String authCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authCode);

        return restClient.post()
                .uri(tokenUri)
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(KakaoTokenResponse.class);
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);

    }
}
