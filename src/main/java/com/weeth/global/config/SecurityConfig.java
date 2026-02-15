package com.weeth.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeth.domain.user.domain.service.UserGetService;
import com.weeth.global.auth.authentication.CustomAccessDeniedHandler;
import com.weeth.global.auth.authentication.CustomAuthenticationEntryPoint;
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase;
import com.weeth.global.auth.jwt.filter.JwtAuthenticationProcessingFilter;
import com.weeth.global.auth.jwt.service.JwtProvider;
import com.weeth.global.auth.jwt.service.JwtService;
import com.weeth.global.auth.login.filter.CustomJsonUsernamePasswordAuthenticationFilter;
import com.weeth.global.auth.login.handler.LoginFailureHandler;
import com.weeth.global.auth.login.handler.LoginSuccessHandler;
import com.weeth.global.auth.login.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final LoginService loginService;
    private final JwtProvider jwtProvider;
    private final JwtService jwtService;
    private final JwtManageUseCase jwtManageUseCase;
    private final UserGetService userGetService;
    private final ObjectMapper objectMapper;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(
                        headersConfigurer ->
                                headersConfigurer
                                        .frameOptions(
                                                HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                        )
                )
                // 세션 사용하지 않으므로 STATELESS로 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //== URL별 권한 관리 옵션 ==//
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/api/v1/users/kakao/login", "api/v1/users/kakao/register", "api/v1/users/kakao/link", "/api/v1/users/apple/login", "/api/v1/users/apple/register", "/api/v1/users/apply", "/api/v1/users/email", "/api/v1/users/refresh").permitAll()
                                        .requestMatchers("/health-check").permitAll()
                                        .requestMatchers("/oauth2/**", "/.well-known/**", "/kakao/oauth", "/apple/oauth").permitAll()
                                        .requestMatchers("/admin", "/admin/login", "/admin/account", "/admin/meeting", "/admin/member", "/admin/penalty",
                                                "/js/**", "/img/**", "/scss/**", "/vendor/**").permitAll()
                                        // 스웨거 경로
                                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/swagger/**").permitAll()
                                        .requestMatchers("/actuator/prometheus")
                                            .access((authentication, context) -> {
                                                String ip = context.getRequest().getRemoteAddr();
                                                boolean allowed = ip.startsWith("172.") || ip.equals("127.0.0.1");
                                                return new AuthorizationDecision(allowed);
                                            })
                                        .requestMatchers("/actuator/health").permitAll()
                                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                        .anyRequest().authenticated()
                )

                .addFilterAfter(customJsonUsernamePasswordAuthenticationFilter(), LogoutFilter.class)
                .addFilterBefore(jwtAuthenticationProcessingFilter(), CustomJsonUsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(customAuthenticationEntryPoint)
                                .accessDeniedHandler(customAccessDeniedHandler))
                .build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "https://weeth.kr", "https://www.weeth.kr", "https://dev.weeth.kr", "https://develop.dl97snxjdgiq1.amplifyapp.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Authorization_refresh"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(loginService);
        return new ProviderManager(provider);
    }

    @Bean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(jwtManageUseCase, userGetService);
    }

    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }

    @Bean
    public CustomJsonUsernamePasswordAuthenticationFilter customJsonUsernamePasswordAuthenticationFilter() {
        CustomJsonUsernamePasswordAuthenticationFilter customJsonUsernamePasswordLoginFilter
                = new CustomJsonUsernamePasswordAuthenticationFilter(objectMapper);
        customJsonUsernamePasswordLoginFilter.setAuthenticationManager(authenticationManager());
        customJsonUsernamePasswordLoginFilter.setAuthenticationSuccessHandler(loginSuccessHandler());
        customJsonUsernamePasswordLoginFilter.setAuthenticationFailureHandler(loginFailureHandler());
        return customJsonUsernamePasswordLoginFilter;
    }

    @Bean
    public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter() {
        return new JwtAuthenticationProcessingFilter(jwtProvider, jwtService, userGetService);
    }
}
