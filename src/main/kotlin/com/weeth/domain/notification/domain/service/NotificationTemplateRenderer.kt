package com.weeth.domain.notification.domain.service

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.NotificationTemplateProvider
import com.weeth.domain.notification.domain.vo.NotificationRenderingResult
import com.weeth.domain.notification.domain.vo.RenderedNotification
import org.springframework.stereotype.Service

@Service
class NotificationTemplateRenderer(
    private val provider: NotificationTemplateProvider,
) {
    fun render(
        type: NotificationType,
        channel: NotificationChannel,
        variables: Map<String, String?>,
    ): NotificationRenderingResult {
        val template =
            provider.find(type, channel)
                ?: return NotificationRenderingResult.Failure(NotificationTemplateFailureReason.TEMPLATE_NOT_FOUND)
        val sources = listOf(template.titleTemplate, template.bodyTemplate)
        val placeholders =
            sources
                .flatMap { source ->
                    PLACEHOLDER.findAll(source).map { it.groupValues[1] }.toList()
                }.toSet()
        val malformed = sources.any { source -> PLACEHOLDER.replace(source, "").any { it == '{' || it == '}' } }
        if (template.version <= 0 || malformed || !template.requiredVariables.containsAll(placeholders)) {
            return NotificationRenderingResult.Failure(
                NotificationTemplateFailureReason.INVALID_TEMPLATE,
                templateVersion = template.version,
            )
        }
        val missing = template.requiredVariables.filterTo(sortedSetOf()) { variables[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            return NotificationRenderingResult.Failure(
                NotificationTemplateFailureReason.MISSING_VARIABLES,
                missing,
                template.version,
            )
        }
        // 단일 패스로 치환해 사용자가 쓴 {변수명}이나 달러 문자를 재해석하지 않는다.
        val rendered =
            sources.map { source ->
                PLACEHOLDER.replace(source) { checkNotNull(variables[it.groupValues[1]]) }.trim()
            }
        val (title, body) = rendered
        if (title.isBlank() || title.length > MAX_TITLE_LENGTH || body.isBlank()) {
            return NotificationRenderingResult.Failure(
                NotificationTemplateFailureReason.INVALID_MESSAGE,
                templateVersion = template.version,
            )
        }
        val displayBody = if (body.length > MAX_BODY_LENGTH) body.take(MAX_BODY_LENGTH - 1) + "…" else body
        return NotificationRenderingResult.Success(RenderedNotification(title, displayBody, template.version))
    }

    private companion object {
        val PLACEHOLDER = Regex("\\{([a-zA-Z][a-zA-Z0-9_]*)}")
        const val MAX_TITLE_LENGTH = 100
        const val MAX_BODY_LENGTH = 255
    }
}
