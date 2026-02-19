package com.weeth.global.auth.jwt.filter

import com.weeth.global.auth.jwt.application.exception.TokenNotFoundException
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.auth.model.AuthenticatedUser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationProcessingFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenExtractor: JwtTokenExtractor,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val accessToken = jwtTokenExtractor.extractAccessToken(request) ?: throw TokenNotFoundException()
            jwtTokenProvider.validate(accessToken)
            saveAuthentication(accessToken)
        } catch (e: TokenNotFoundException) {
            log.debug("Token not found: {}", e.message)
        } catch (e: RuntimeException) {
            log.info("error token: {}", e.message)
        }

        filterChain.doFilter(request, response)
    }

    private fun saveAuthentication(accessToken: String) {
        val claims = jwtTokenExtractor.extractClaims(accessToken) ?: throw TokenNotFoundException()
        val principal = AuthenticatedUser(claims.id, claims.email, claims.role)

        val authentication =
            UsernamePasswordAuthenticationToken(
                principal,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${claims.role.name}")),
            )

        SecurityContextHolder.getContext().authentication = authentication
    }
}
