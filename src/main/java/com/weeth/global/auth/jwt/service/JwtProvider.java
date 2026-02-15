package com.weeth.global.auth.jwt.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.global.auth.jwt.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String EMAIL_CLAIM = "email";
    private static final String ID_CLAIM = "id";
    private static final String ROLE_CLAIM = "role";

    @Value("${weeth.jwt.key}")
    private String key;
    @Value("${weeth.jwt.access.expiration}")
    private Long accessTokenExpirationPeriod;
    @Value("${weeth.jwt.refresh.expiration}")
    private Long refreshTokenExpirationPeriod;

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public String createAccessToken(Long id, String email, Role role) {
        Date now = new Date();
        return JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + accessTokenExpirationPeriod))
                .withClaim(ID_CLAIM, id)
                .withClaim(EMAIL_CLAIM, email)
                .withClaim(ROLE_CLAIM, role.toString())
                .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    public String createRefreshToken(Long id) {
        Date now = new Date();
        return JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + refreshTokenExpirationPeriod))
                .withClaim(ID_CLAIM, id)
                .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    public boolean validate(String token) {
        try {
            JWT.require(Algorithm.RSA256(publicKey)).build().verify(token);
            return true;
        } catch (Exception e) {
            log.error("유효하지 않은 토큰입니다. {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }

}
