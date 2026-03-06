package com.weeth.global.common.web

import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.core.MethodParameter
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.HandlerMapping

/**
 * `@TsidPathVariable`가 선언된 path variable을 Base62 TSID 문자열에서 Long 값으로 변환한다.
 *
 * 제약:
 * - 파라미터 타입은 `Long` 또는 `long`만 지원한다.
 * - 어노테이션 `value`가 비어 있으면 파라미터 이름을 path variable 이름으로 사용한다.
 * - 디코딩 실패 시 `InvalidTsidPathVariableException`을 던진다.
 */
class TsidPathVariableArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(TsidPathVariable::class.java)
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
        val annotation =
            parameter.getParameterAnnotation(TsidPathVariable::class.java)
                ?: throw IllegalStateException("@TsidPathVariable 어노테이션이 필요합니다.")

        val variableName =
            annotation.value.ifBlank {
                parameter.parameterName ?: throw IllegalStateException("PathVariable 이름을 해석할 수 없습니다.")
            }

        val uriVariables =
            webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST,
            ) as? Map<*, *> ?: emptyMap<String, String>()

        val rawValue =
            uriVariables[variableName] as? String ?: throw MissingPathVariableException(variableName, parameter)

        return try {
            TsidBase62Encoder.decode(rawValue)
        } catch (e: IllegalArgumentException) {
            throw InvalidTsidPathVariableException(
                variableName = variableName,
                value = rawValue,
                cause = e,
            )
        }
    }
}

class InvalidTsidPathVariableException(
    val variableName: String,
    val value: String,
    cause: Throwable? = null,
) : ServletRequestBindingException(
        "유효하지 않은 TSID 경로 변수 '$variableName': $value",
        cause,
    )
