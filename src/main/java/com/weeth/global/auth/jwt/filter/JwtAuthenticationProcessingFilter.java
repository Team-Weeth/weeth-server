package com.weeth.global.auth.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.service.UserGetService;
import com.weeth.global.auth.jwt.exception.TokenNotFoundException;
import com.weeth.global.auth.jwt.service.JwtProvider;
import com.weeth.global.auth.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    private static final String NO_CHECK_URL = "/api/v1/login";
    private final String DUMMY = "DUMMY_PASSWORD";

    private final JwtProvider jwtProvider;
    private final JwtService jwtService;
    private final UserGetService userGetService;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

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

        String email = jwtService.extractEmail(accessToken).get();
        Role role = Role.valueOf(jwtService.extractRole(accessToken).get());

        UserDetails userDetailsUser = org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password(DUMMY)
                .roles(role.name())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetailsUser, null,
                        authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
