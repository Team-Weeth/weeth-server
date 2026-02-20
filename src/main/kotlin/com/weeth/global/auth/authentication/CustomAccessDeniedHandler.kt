package com.weeth.global.auth.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.global.common.response.CommonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        setResponse(response)
        log.error(
            "ExceptionClass: {}, Message: {}",
            accessDeniedException::class.simpleName,
            accessDeniedException.message,
        )
    }

    private fun setResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val message =
            objectMapper.writeValueAsString(
                CommonResponse.createFailure(
                    ErrorMessage.FORBIDDEN.code,
                    ErrorMessage.FORBIDDEN.message,
                ),
            )
        response.writer.write(message)
    }
}
