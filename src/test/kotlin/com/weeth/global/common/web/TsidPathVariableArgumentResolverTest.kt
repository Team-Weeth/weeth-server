package com.weeth.global.common.web

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.HandlerMapping

class TsidPathVariableArgumentResolverTest :
    StringSpec({
        val resolver = TsidPathVariableArgumentResolver()

        "@TsidPathVariable Long 파라미터를 지원한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)

            resolver.supportsParameter(parameter) shouldBe true
        }

        "Base62 path variable을 Long으로 디코딩한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)
            val request = MockHttpServletRequest()
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("clubId" to "1zA9"))

            val result = resolver.resolveArgument(parameter, null, ServletWebRequest(request), null)

            result shouldBe 375_109L
        }

        "유효하지 않은 Base62 값이면 예외가 발생한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)
            val request = MockHttpServletRequest()
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("clubId" to "%%%"))

            shouldThrow<InvalidTsidPathVariableException> {
                resolver.resolveArgument(parameter, null, ServletWebRequest(request), null)
            }
        }

        "path variable이 누락되면 예외가 발생한다" {
            val method = DummyController::class.java.getDeclaredMethod("target", java.lang.Long.TYPE)
            val parameter = MethodParameter(method, 0)
            val request = MockHttpServletRequest()
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, emptyMap<String, String>())

            shouldThrow<MissingPathVariableException> {
                resolver.resolveArgument(parameter, null, ServletWebRequest(request), null)
            }
        }
    }) {
    private class DummyController {
        @Suppress("unused")
        fun target(
            @TsidPathVariable("clubId") clubId: Long,
        ) {
            clubId.toString()
        }
    }
}
