package com.weeth.global.auth.resolver

import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.AnonymousAuthenticationException
import com.weeth.global.auth.model.AuthenticatedUser
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentUser::class.java)
        val parameterType = parameter.parameterType
        val isLongType = parameterType == Long::class.java || parameterType == Long::class.javaPrimitiveType
        return hasAnnotation && isLongType
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || authentication is AnonymousAuthenticationToken) {
            throw AnonymousAuthenticationException()
        }

        val principal = authentication.principal

        if (principal is AuthenticatedUser) {
            return principal.id
        }

        throw AnonymousAuthenticationException()
    }
}
