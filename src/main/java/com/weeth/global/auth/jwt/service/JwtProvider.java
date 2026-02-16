package com.weeth.global.auth.jwt.service;

import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.global.auth.jwt.exception.InvalidTokenException;
import com.weeth.global.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtProvider {

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String EMAIL_CLAIM = "email";
    private static final String ID_CLAIM = "id";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final Long accessTokenExpirationPeriod;
    private final Long refreshTokenExpirationPeriod;

    public JwtProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getKey().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationPeriod = jwtProperties.getAccess().getExpiration();
        this.refreshTokenExpirationPeriod = jwtProperties.getRefresh().getExpiration();
    }


    public String createAccessToken(Long id, String email, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .claim(ID_CLAIM, id)
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_CLAIM, role.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationPeriod))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long id) {
        Date now = new Date();
        return Jwts.builder()
                .subject(REFRESH_TOKEN_SUBJECT)
                .claim(ID_CLAIM, id)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpirationPeriod))
                .signWith(secretKey)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("유효하지 않은 토큰입니다. {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }
}
