package com.weeth.global.config

import com.weeth.global.auth.authentication.CustomAccessDeniedHandler
import com.weeth.global.auth.authentication.CustomAuthenticationEntryPoint
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.port.AccessTokenBlacklistStorePort
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.auth.jwt.filter.JwtAuthenticationProcessingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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
    fun filterChain(
        http: HttpSecurity,
        jwtAuthenticationProcessingFilter: JwtAuthenticationProcessingFilter,
    ): SecurityFilterChain =
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
                        "/api/v4/users/social/kakao",
                        "/api/v4/users/social/apple",
                        "/api/v4/users/social/apple/callback",
                        "/api/v4/users/social/refresh",
                        "/api/v4/users/inquiries",
                    ).permitAll()
                    .requestMatchers("/health-check")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v4/clubs/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v4/university/*")
                    .permitAll()
                    .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger/**",
                    ).permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .access { _, context ->
                        val address = java.net.InetAddress.getByName(context.request.remoteAddr)
                        AuthorizationDecision(address.isLoopbackAddress || address.isSiteLocalAddress)
                    }.requestMatchers("/api/v4/users/terms")
                    .hasAnyRole("TEMPORARY", "USER")
                    .anyRequest()
                    .hasRole("USER")
            }.exceptionHandling { exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns =
                    listOf(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://*.v4.weeth.kr",
                        "https://landing.weeth.kr",
                        "https://www.landing.weeth.kr",
                        "https://weeth.kr",
                        "https://www.weeth.kr",
                        "https://appleid.apple.com",
                    )
                allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                exposedHeaders = listOf("Authorization", "Authorization_refresh")
                allowCredentials = true
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun jwtAuthenticationProcessingFilter(
        accessTokenBlacklistStore: AccessTokenBlacklistStorePort,
    ): JwtAuthenticationProcessingFilter =
        JwtAuthenticationProcessingFilter(jwtTokenProvider, jwtTokenExtractor, accessTokenBlacklistStore)
}
