package com.weeth.global.auth.resolver

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.annotation.CurrentUserRole
import com.weeth.global.auth.jwt.application.exception.AnonymousAuthenticationException
import com.weeth.global.auth.model.AuthenticatedUser
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class CurrentUserRoleArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentUserRole::class.java)
        val parameterType = Role::class.java.isAssignableFrom(parameter.parameterType)
        return hasAnnotation && parameterType
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
            return principal.role
        }

        val role =
            authentication.authorities
                .asSequence()
                .mapNotNull { authority -> authority.authority }
                .filter { it.startsWith("ROLE_") }
                .mapNotNull { raw ->
                    runCatching { Role.valueOf(raw.removePrefix("ROLE_")) }.getOrNull()
                }.firstOrNull()

        return role ?: throw AnonymousAuthenticationException()
    }
}
