package com.weeth.global.auth.jwt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import com.weeth.global.auth.jwt.exception.TokenNotFoundException;
import com.weeth.global.common.response.CommonResponse;
import com.weeth.global.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;

    public String extractRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(jwtProperties.getRefresh().getHeader()))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, ""))
                .orElseThrow(TokenNotFoundException::new);
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(jwtProperties.getAccess().getHeader()))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, ""));
    }

    public Optional<String> extractEmail(String accessToken) {
        try {
            Claims claims = jwtProvider.parseClaims(accessToken);
            return Optional.ofNullable(claims.get(EMAIL_CLAIM, String.class));
        } catch (Exception e) {
            log.error("액세스 토큰이 유효하지 않습니다.");
            return Optional.empty();
        }
    }

    public Optional<Long> extractId(String token) {
        try {
            Claims claims = jwtProvider.parseClaims(token);
            return Optional.ofNullable(claims.get(ID_CLAIM, Long.class));
        } catch (Exception e) {
            log.error("액세스 토큰이 유효하지 않습니다.");
            return Optional.empty();
        }
    }

    public Optional<String> extractRole(String token) {
        try {
            Claims claims = jwtProvider.parseClaims(token);
            return Optional.ofNullable(claims.get(ROLE_CLAIM, String.class));
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
