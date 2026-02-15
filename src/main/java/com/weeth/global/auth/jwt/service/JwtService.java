package com.weeth.global.auth.jwt.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import com.weeth.global.auth.jwt.exception.TokenNotFoundException;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String EMAIL_CLAIM = "email";
    private static final String ID_CLAIM = "id";
    private static final String ROLE_CLAIM = "role";
    private static final String BEARER = "Bearer ";
    private static final String LOGIN_SUCCESS_MESSAGE = "자체 로그인 성공.";

    @Value("${weeth.jwt.key}")
    private String key;
    @Value("${weeth.jwt.access.header}")
    private String accessHeader;
    @Value("${weeth.jwt.refresh.header}")
    private String refreshHeader;

    private final RSAPublicKey publicKey;

    public String extractRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(refreshHeader))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, ""))
                .orElseThrow(TokenNotFoundException::new);
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(accessHeader))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, ""));
    }

    public Optional<String> extractEmail(String accessToken) {
        try {
            return Optional.ofNullable(JWT.require(Algorithm.RSA256(publicKey))
                    .build()
                    .verify(accessToken)
                    .getClaim(EMAIL_CLAIM)
                    .asString());
        } catch (Exception e) {
            log.error("액세스 토큰이 유효하지 않습니다.");
            return Optional.empty();
        }
    }

    public Optional<Long> extractId(String token) {
        try {
            return Optional.ofNullable(JWT.require(Algorithm.RSA256(publicKey))
                    .build()
                    .verify(token)
                    .getClaim(ID_CLAIM)
                    .asLong());
        } catch (Exception e) {
            log.error("액세스 토큰이 유효하지 않습니다.");
            return Optional.empty();
        }
    }

    public Optional<String> extractRole(String token) {
        try {
            return Optional.ofNullable(JWT.require(Algorithm.RSA256(publicKey))
                    .build()
                    .verify(token)
                    .getClaim(ROLE_CLAIM)
                    .asString());
        } catch (Exception e) {
            log.error("액세스 토큰이 유효하지 않습니다.");
            return Optional.empty();
        }
    }

    // header -> body로 수정
    public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken, String refreshToken) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = new ObjectMapper().writeValueAsString(CommonResponse.createSuccess(LOGIN_SUCCESS_MESSAGE, new JwtDto(accessToken, refreshToken)));
        response.getWriter().write(message);
    }

}
