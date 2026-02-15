package com.weeth.global.auth.apple;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.weeth.global.auth.apple.dto.ApplePublicKey;
import com.weeth.global.auth.apple.dto.ApplePublicKeys;
import com.weeth.global.auth.apple.dto.AppleTokenResponse;
import com.weeth.global.auth.apple.dto.AppleUserInfo;
import com.weeth.global.auth.apple.exception.AppleAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class AppleAuthService {

    @Value("${auth.providers.apple.client_id}")
    private String appleClientId;

    @Value("${auth.providers.apple.team_id}")
    private String appleTeamId;

    @Value("${auth.providers.apple.key_id}")
    private String appleKeyId;

    @Value("${auth.providers.apple.redirect_uri}")
    private String redirectUri;

    @Value("${auth.providers.apple.token_uri}")
    private String tokenUri;

    @Value("${auth.providers.apple.keys_uri}")
    private String keysUri;

    @Value("${auth.providers.apple.private_key_path}")
    private String privateKeyPath;

    @Value("${auth.providers.apple.allowed_audiences}")
    private java.util.List<String> allowedAudiences;

    private final RestClient restClient = RestClient.create();

    /**
     * Authorization code로 애플 토큰 요청
     * client_secret은 JWT로 생성 (ES256 알고리즘)
     */
    public AppleTokenResponse getAppleToken(String authCode) {
        String clientSecret = generateClientSecret();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", appleClientId);
        body.add("client_secret", clientSecret);
        body.add("code", authCode);
        body.add("redirect_uri", redirectUri);

        return restClient.post()
                .uri(tokenUri)
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(AppleTokenResponse.class);
    }

    /**
     * ID Token 검증 및 사용자 정보 추출
     * 애플은 별도 userInfo 엔드포인트가 없고 ID Token에 정보가 포함됨
     */
    public AppleUserInfo verifyAndDecodeIdToken(String idToken) {
        try {
            // 1. ID Token의 헤더에서 kid 추출
            String[] tokenParts = idToken.split("\\.");
            String header = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            Map<String, Object> headerMap = parseJson(header);
            String kid = (String) headerMap.get("kid");

            // 2. 애플 공개키 가져오기
            ApplePublicKeys publicKeys = restClient.get()
                    .uri(keysUri)
                    .retrieve()
                    .body(ApplePublicKeys.class);

            // 3. kid와 일치하는 공개키 찾기
            ApplePublicKey matchedKey = publicKeys.keys().stream()
                    .filter(key -> key.kid().equals(kid))
                    .findFirst()
                    .orElseThrow(AppleAuthenticationException::new);

            // 4. 공개키로 ID Token 검증
            PublicKey publicKey = generatePublicKey(matchedKey);
            // JJWT 0.13.0+ uses parser() instead of parserBuilder()
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

            // 5. Claims 검증
            validateClaims(claims);

            // 6. 사용자 정보 추출
            String appleId = claims.getSubject();
            String email = claims.get("email", String.class);
            Boolean emailVerified = claims.get("email_verified", Boolean.class);

            return AppleUserInfo.builder()
                    .appleId(appleId)
                    .email(email)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .build();

        } catch (Exception e) {
            log.error("애플 ID Token 검증 실패", e);
            throw new AppleAuthenticationException();
        }
    }

    /**
     * 애플 로그인용 client_secret 생성
     * ES256 알고리즘으로 JWT 생성 (p8 키 파일 사용)
     */
    private String generateClientSecret() {
        try (InputStream inputStream = getInputStream(privateKeyPath)) {
            // p8 파일에서 Private Key 읽기
            String privateKeyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // PEM 형식의 헤더/푸터 제거
            privateKeyContent = privateKeyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // Private Key 객체 생성
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            );

            // JWT 생성
            LocalDateTime now = LocalDateTime.now();
            Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
            Date expiration = Date.from(now.plusMonths(5).atZone(ZoneId.systemDefault()).toInstant());

            return Jwts.builder()
                    .setHeaderParam("kid", appleKeyId)
                    .setHeaderParam("alg", "ES256")
                    .setIssuer(appleTeamId)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiration)
                    .setAudience("https://appleid.apple.com")
                    .setSubject(appleClientId)
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();

        } catch (Exception e) {
            log.error("애플 Client Secret 생성 실패", e);
            throw new AppleAuthenticationException();
        }
    }

    /**
     * 파일 경로에서 InputStream 가져오기
     * 절대 경로면 파일 시스템에서, 상대 경로면 classpath에서 읽음
     */
    private InputStream getInputStream(String path) throws IOException {
        // 절대 경로인 경우 파일 시스템에서 읽기
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*")) {
            return new FileInputStream(path);
        }
        // 상대 경로는 classpath에서 읽기
        return new ClassPathResource(path).getInputStream();
    }

    /**
     * 애플 공개키로부터 PublicKey 객체 생성
     */
    private PublicKey generatePublicKey(ApplePublicKey applePublicKey) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(applePublicKey.n());
            byte[] eBytes = Base64.getUrlDecoder().decode(applePublicKey.e());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception ex) {
            log.error("애플 공개키 생성 실패", ex);
            throw new AppleAuthenticationException();
        }
    }

    /**
     * ID Token의 Claims 검증
     */
    private void validateClaims(Claims claims) {
        String iss = claims.getIssuer();
        // JJWT 0.13.0+ returns Set<String> for getAudience()
        var audSet = claims.getAudience();
        String aud = audSet.iterator().hasNext() ? audSet.iterator().next() : null;

        if (!iss.equals("https://appleid.apple.com")) {
            throw new RuntimeException("유효하지 않은 발급자(issuer)입니다.");
        }

        // 허용된 audience 목록에 포함되어 있는지 확인 (웹 + Leenk 앱)
        if (aud == null || !allowedAudiences.contains(aud)) {
            log.error("유효하지 않은 audience: {}. 허용된 목록: {}", aud, allowedAudiences);
            throw new RuntimeException("유효하지 않은 수신자(audience)입니다.");
        }

        Date expiration = claims.getExpiration();
        if (expiration.before(new Date())) {
            throw new RuntimeException("만료된 ID Token입니다.");
        }
    }

    /**
     * JSON 문자열을 Map으로 파싱
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패");
        }
    }
}
