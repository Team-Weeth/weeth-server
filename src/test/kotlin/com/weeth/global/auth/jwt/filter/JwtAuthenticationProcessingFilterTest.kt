package com.weeth.global.auth.jwt.filter

import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
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
            it("유효한 토큰이면 SecurityContext에 인증을 저장한다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v1/users" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns "access-token"
                every { jwtProvider.validate("access-token") } just runs
                every { jwtService.extractClaims("access-token") } returns JwtTokenExtractor.TokenClaims(1L, "admin@weeth.com", "ADMIN")

                filter.doFilter(request, response, chain)

                val authentication = SecurityContextHolder.getContext().authentication
                (authentication == null) shouldBe false
                (authentication.principal is AuthenticatedUser) shouldBe true
                val principal = authentication.principal as AuthenticatedUser
                principal.id shouldBe 1L
                principal.email shouldBe "admin@weeth.com"
                principal.role.name shouldBe "ADMIN"
                authentication.authorities.any { it.authority == "ROLE_ADMIN" } shouldBe true
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

            it("role claim이 유효하지 않으면 인증을 저장하지 않는다") {
                val request = MockHttpServletRequest().apply { requestURI = "/api/v1/users" }
                val response = MockHttpServletResponse()
                val chain = MockFilterChain()

                every { jwtService.extractAccessToken(request) } returns "access-token"
                every { jwtProvider.validate("access-token") } just runs
                every {
                    jwtService.extractClaims("access-token")
                } returns JwtTokenExtractor.TokenClaims(1L, "admin@weeth.com", "NOT_A_ROLE")

                filter.doFilter(request, response, chain)

                SecurityContextHolder.getContext().authentication shouldBe null
            }
        }
    })
