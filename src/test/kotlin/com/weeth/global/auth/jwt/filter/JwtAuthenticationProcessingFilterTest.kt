package com.weeth.global.auth.jwt.filter

import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.enums.TokenType
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.auth.model.AuthenticatedUser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationProcessingFilterTest :
    DescribeSpec({
        val jwtProvider = mockk<JwtTokenProvider>()
        val jwtService = mockk<JwtTokenExtractor>()
        val filter = JwtAuthenticationProcessingFilter(jwtProvider, jwtService)

        beforeTest {
            SecurityContextHolder.clearContext()
            clearMocks(jwtProvider, jwtService)
        }

        afterTest {
            SecurityContextHolder.clearContext()
        }

        describe("doFilterInternal") {
            it("ACCESS 토큰이면 ROLE_USER 권한을 부여한다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v1/users" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns "access-token"
                every { jwtProvider.validate("access-token") } just runs
                every { jwtService.extractClaims("access-token") } returns
                    JwtTokenExtractor.TokenClaims(1L, "admin@weeth.com", TokenType.ACCESS)

                filter.doFilter(request, response, chain)

                val authentication = SecurityContextHolder.getContext().authentication
                (authentication == null) shouldBe false
                (authentication.principal is AuthenticatedUser) shouldBe true
                val principal = authentication.principal as AuthenticatedUser
                principal.id shouldBe 1L
                principal.email shouldBe "admin@weeth.com"
                authentication.authorities.any { it.authority == "ROLE_USER" } shouldBe true
            }

            it("TEMPORARY 토큰이면 ROLE_TEMPORARY 권한을 부여한다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v4/users/terms" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns "temp-token"
                every { jwtProvider.validate("temp-token") } just runs
                every { jwtService.extractClaims("temp-token") } returns
                    JwtTokenExtractor.TokenClaims(2L, "new@weeth.com", TokenType.TEMPORARY)

                filter.doFilter(request, response, chain)

                val authentication = SecurityContextHolder.getContext().authentication
                (authentication == null) shouldBe false
                authentication.authorities.any { it.authority == "ROLE_TEMPORARY" } shouldBe true
                authentication.authorities.any { it.authority == "ROLE_USER" } shouldBe false
            }

            it("토큰이 없으면 인증을 저장하지 않는다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v1/users" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns null

                filter.doFilter(request, response, chain)

                SecurityContextHolder.getContext().authentication shouldBe null
                verify(exactly = 0) { jwtProvider.validate(any()) }
            }

            it("claims 추출에 실패하면 인증을 저장하지 않는다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v1/users" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns "access-token"
                every { jwtProvider.validate("access-token") } just runs
                every { jwtService.extractClaims("access-token") } returns null

                filter.doFilter(request, response, chain)

                SecurityContextHolder.getContext().authentication shouldBe null
            }
        }
    })
