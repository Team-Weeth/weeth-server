package com.weeth.global.config

import com.weeth.global.auth.authentication.CustomAccessDeniedHandler
import com.weeth.global.auth.authentication.CustomAuthenticationEntryPoint
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.auth.jwt.filter.JwtAuthenticationProcessingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenExtractor: JwtTokenExtractor,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .cors(withDefaults())
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { frameOptions -> frameOptions.sameOrigin() }
            }.sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/api/v4/users/apply",
                        "/api/v4/users/email",
                        "/api/v4/users/social/kakao",
                        "/api/v4/users/social/apple",
                        "/api/v4/users/social/refresh",
                        "/api/v1/users/apply",
                        "/api/v1/users/email",
                    ).permitAll()
                    .requestMatchers("/health-check")
                    .permitAll()
                    .requestMatchers(
                        "/admin",
                        "/admin/login",
                        "/admin/account",
                        "/admin/meeting",
                        "/admin/member",
                        "/admin/penalty",
                    ).permitAll()
                    .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger/**",
                    ).permitAll()
                    .requestMatchers("/actuator/prometheus")
                    .access { _, context ->
                        val ip = context.request.remoteAddr
                        val allowed = ip.startsWith("172.") || ip == "127.0.0.1"
                        AuthorizationDecision(allowed)
                    }.requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/admin/**",
                        "/api/v4/admin/**",
                    ).hasRole("ADMIN")
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling { exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
                allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                exposedHeaders = listOf("Authorization", "Authorization_refresh")
                allowCredentials = true
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun jwtAuthenticationProcessingFilter(): JwtAuthenticationProcessingFilter =
        JwtAuthenticationProcessingFilter(jwtTokenProvider, jwtTokenExtractor)
}
