package com.weeth.global.auth.resolver

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.AnonymousAuthenticationException
import com.weeth.global.auth.model.AuthenticatedUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.ServletWebRequest

class CurrentUserArgumentResolverTest :
    StringSpec({
        val resolver = CurrentUserArgumentResolver()

        afterTest {
            SecurityContextHolder.clearContext()
        }

        "@CurrentUser Long 파라미터를 지원한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)

            resolver.supportsParameter(parameter) shouldBe true
        }

        "인증 컨텍스트가 익명이면 예외가 발생한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)
            val request = MockHttpServletRequest()

            SecurityContextHolder.getContext().authentication =
                AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

            shouldThrow<AnonymousAuthenticationException> {
                resolver.resolveArgument(parameter, null, ServletWebRequest(request), null)
            }
        }

        "principal이 AuthenticatedUser면 userId를 반환한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)
            val request = MockHttpServletRequest()
            val principal = AuthenticatedUser(id = 99L, email = "test@weeth.com", role = Role.USER)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(principal, null, emptyList())

            val result = resolver.resolveArgument(parameter, null, ServletWebRequest(request), null)

            result shouldBe 99L
        }
    }) {
    private class DummyController {
        @Suppress("unused")
        fun target(
            @CurrentUser userId: Long,
        ) {
            userId.toString()
        }
    }
}
