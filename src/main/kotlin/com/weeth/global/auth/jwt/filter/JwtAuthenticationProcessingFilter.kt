package com.weeth.global.auth.jwt.filter

import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.exception.TokenNotFoundException
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.enums.TokenType
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.auth.model.AuthenticatedUser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationProcessingFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenExtractor: JwtTokenExtractor,
    private val userReader: UserReader,
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
        validateUserStatus(claims.id)
        val principal = AuthenticatedUser(claims.id, claims.email)

        val role =
            when (claims.tokenType) {
                TokenType.TEMPORARY -> "ROLE_TEMPORARY"
                TokenType.ACCESS -> "ROLE_USER"
            }

        val authentication =
            UsernamePasswordAuthenticationToken(
                principal,
                null,
                listOf(SimpleGrantedAuthority(role)),
            )

        SecurityContextHolder.getContext().authentication = authentication
        MDC.put("userId", claims.id.toString())
    }

    private fun validateUserStatus(userId: Long) {
        val user = userReader.getById(userId)
        if (user.isBannedOrLeft()) throw UserInActiveException()
    }
}
