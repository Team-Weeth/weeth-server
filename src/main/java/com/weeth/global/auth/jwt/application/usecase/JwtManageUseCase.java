package com.weeth.global.auth.jwt.application.usecase;

import jakarta.servlet.http.HttpServletResponse;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import com.weeth.global.auth.jwt.service.JwtProvider;
import com.weeth.global.auth.jwt.service.JwtRedisService;
import com.weeth.global.auth.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class JwtManageUseCase {

    private final JwtProvider jwtProvider;
    private final JwtService jwtService;
    private final JwtRedisService jwtRedisService;

    // 토큰 발급
    public JwtDto create(Long userId, String email, Role role){
        String accessToken = jwtProvider.createAccessToken(userId, email, role);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        updateToken(userId, refreshToken, role, email);

        return new JwtDto(accessToken, refreshToken);
    }

    // 토큰 헤더로 전송
    public void sendToken(JwtDto dto, HttpServletResponse response) throws IOException {
        jwtService.sendAccessAndRefreshToken(response, dto.accessToken(), dto.refreshToken());
    }

    // 토큰 재발급
    public JwtDto reIssueToken(String requestToken){
        jwtProvider.validate(requestToken);

        Long userId = jwtService.extractId(requestToken).get();

        jwtRedisService.validateRefreshToken(userId, requestToken);

        Role role = jwtRedisService.getRole(userId);
        String email = jwtRedisService.getEmail(userId);

        JwtDto token = create(userId, email, role);
        jwtRedisService.set(userId, token.refreshToken(), role, email);

        return token;
    }

    // 리프레시 토큰 업데이트
    private void updateToken(long userId, String refreshToken, Role role, String email){
        jwtRedisService.set(userId, refreshToken, role, email);
    }

}
