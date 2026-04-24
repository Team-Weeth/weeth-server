package com.weeth.global.auth.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.response.CommonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    private val errorLog = LoggerFactory.getLogger("ERROR_LOG")

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        if (isTemporaryUser()) {
            setRegistrationIncompleteResponse(response)
        } else {
            setForbiddenResponse(response)
        }

        try {
            MDC.put("status", response.status.toString())
            MDC.put("errorType", accessDeniedException::class.simpleName ?: "AccessDeniedException")
            MDC.put("errorMessage", accessDeniedException.message ?: ErrorMessage.FORBIDDEN.message)
            errorLog.warn("Access Denied")
        } finally {
            MDC.remove("status")
            MDC.remove("errorType")
            MDC.remove("errorMessage")
        }
    }

    private fun isTemporaryUser(): Boolean =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.any { it.authority == "ROLE_TEMPORARY" }
            ?: false

    private fun setRegistrationIncompleteResponse(response: HttpServletResponse) {
        val errorCode = JwtErrorCode.REGISTRATION_INCOMPLETE
        response.status = errorCode.status.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val body =
            objectMapper.writeValueAsString(
                CommonResponse.error(errorCode),
            )
        response.writer.write(body)
    }

    private fun setForbiddenResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val body =
            objectMapper.writeValueAsString(
                CommonResponse.createFailure(
                    ErrorMessage.FORBIDDEN.code,
                    ErrorMessage.FORBIDDEN.message,
                ),
            )
        response.writer.write(body)
    }
}
