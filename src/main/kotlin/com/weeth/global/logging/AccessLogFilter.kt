package com.weeth.global.logging

import io.opentelemetry.api.trace.Span
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
class AccessLogFilter : OncePerRequestFilter() {
    private val accessLog = LoggerFactory.getLogger("ACCESS_LOG")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId =
            request.getHeader("X-Request-Id")
                ?: UUID
                    .randomUUID()
                    .toString()
        val startTime = System.currentTimeMillis()

        var failed = false
        try {
            MDC.put("requestId", requestId)
            MDC.put("path", request.requestURI)
            MDC.put("method", request.method)
            putTraceMdc(request)
            response.setHeader("X-Request-Id", requestId)
            filterChain.doFilter(request, response)
        } catch (ex: Throwable) {
            failed = true
            throw ex
        } finally {
            val durationMs = System.currentTimeMillis() - startTime
            val status = if (failed) "500" else response.status.toString()
            MDC.put("status", status)
            MDC.put("durationMs", durationMs.toString())
            putTraceMdc(request)

            accessLog.info("HTTP Request Completed")

            MDC.remove("requestId")
            MDC.remove("path")
            MDC.remove("method")
            MDC.remove("status")
            MDC.remove("durationMs")
            MDC.remove("traceId")
            MDC.remove("spanId")
            MDC.remove("userId")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator") || path.startsWith("/health-check")
    }

    private fun putTraceMdc(request: HttpServletRequest) {
        if (!MDC.get("traceId").isNullOrBlank() && !MDC.get("spanId").isNullOrBlank()) {
            return
        }

        val context = Span.current().spanContext
        if (context.isValid) {
            putTraceContext(context.traceId, context.spanId)
            return
        }

        putTraceparentContext(request.getHeader(TRACEPARENT_HEADER))
    }

    private fun putTraceContext(
        traceId: String?,
        spanId: String?,
    ) {
        if (!traceId.isNullOrBlank()) {
            MDC.put("traceId", traceId)
        }
        if (!spanId.isNullOrBlank()) {
            MDC.put("spanId", spanId)
        }
    }

    private fun putTraceparentContext(traceparent: String?) {
        val parts = traceparent?.split("-") ?: return
        if (parts.size < TRACEPARENT_PARTS) {
            return
        }

        val traceId = parts[TRACEPARENT_TRACE_ID_INDEX]
        val spanId = parts[TRACEPARENT_SPAN_ID_INDEX]
        if (traceId.length == TRACE_ID_LENGTH && spanId.length == SPAN_ID_LENGTH) {
            putTraceContext(traceId, spanId)
        }
    }

    companion object {
        private const val TRACEPARENT_HEADER = "traceparent"
        private const val TRACEPARENT_PARTS = 4
        private const val TRACEPARENT_TRACE_ID_INDEX = 1
        private const val TRACEPARENT_SPAN_ID_INDEX = 2
        private const val TRACE_ID_LENGTH = 32
        private const val SPAN_ID_LENGTH = 16
    }
}
