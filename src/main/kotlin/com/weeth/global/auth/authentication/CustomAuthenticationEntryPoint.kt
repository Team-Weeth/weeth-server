package com.weeth.global.auth.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.global.common.response.CommonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        setResponse(response)
        log.error(
            "ExceptionClass: {}, Message: {}",
            authException::class.simpleName,
            authException.message,
        )
    }

    private fun setResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val message =
            objectMapper.writeValueAsString(
                CommonResponse.createFailure(
                    ErrorMessage.UNAUTHORIZED.code,
                    ErrorMessage.UNAUTHORIZED.message,
                ),
            )
        response.writer.write(message)
    }
}
