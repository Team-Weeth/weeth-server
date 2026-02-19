package com.weeth.global.auth.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.global.auth.jwt.exception.TokenNotFoundException;
import com.weeth.global.auth.model.AuthenticatedUser;
import com.weeth.global.auth.jwt.service.JwtProvider;
import com.weeth.global.auth.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    private static final String NO_CHECK_URL = "/api/v1/login";

    private final JwtProvider jwtProvider;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().equals(NO_CHECK_URL)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 유저 캐싱 도입
        try {
            String accessToken = jwtService.extractAccessToken(request)
                    .orElseThrow(TokenNotFoundException::new);
            if (jwtProvider.validate(accessToken)) {
                saveAuthentication(accessToken);
            }
        } catch (TokenNotFoundException e) {
            log.debug("Token not found: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.info("error token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);

    }

    public void saveAuthentication(String accessToken) {

        Long userId = jwtService.extractId(accessToken).orElseThrow(TokenNotFoundException::new);
        String email = jwtService.extractEmail(accessToken).orElseThrow(TokenNotFoundException::new);
        Role role = Role.valueOf(jwtService.extractRole(accessToken).orElseThrow(TokenNotFoundException::new));
        AuthenticatedUser principal = new AuthenticatedUser(userId, email, role);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
