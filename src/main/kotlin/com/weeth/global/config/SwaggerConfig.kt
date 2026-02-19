package com.weeth.global.config

import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExampleHolder
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.config.properties.JwtProperties
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
@OpenAPIDefinition(
    info =
        Info(
            title = "Weeth API",
            version = "v4.0.0",
            description = """
            ## Response Code 규칙
            - Success: **1xxx**
            - Domain Error: **2xxx**
            - Server Error: **3xxx**
            - Client Error: **4xxx**

            ## 도메인별 코드 범위
            | Domain | Success | Error |
            |--------|---------|------|
            | Account | 11xx | 21xx |
            | Attendance | 12xx | 22xx |
            | Board | 13xx | 23xx |
            | Comment | 14xx | 24xx |
            | File | 15xx | 25xx |
            | Penalty | 16xx | 26xx |
            | Schedule | 17xx | 27xx |
            | User | 18xx | 28xx |
            | Auth/JWT (Global) | - | 29xx |

            > 각 API의 상세 응답 예시는 Swagger의 **Responses** 섹션에서 확인하세요.
            """,
        ),
)
class SwaggerConfig(
    private val jwtProperties: JwtProperties,
) {
    @Bean
    fun openAPI(): OpenAPI {
        val accessSecurityScheme = getAccessSecurityScheme()
        val refreshSecurityScheme = getRefreshSecurityScheme()

        return OpenAPI()
            .addServersItem(Server().url("/"))
            .components(
                Components()
                    .addSecuritySchemes("bearerAuth", accessSecurityScheme)
                    .addSecuritySchemes("refreshBearerAuth", refreshSecurityScheme),
            ).security(
                listOf(
                    SecurityRequirement().addList("bearerAuth"),
                    SecurityRequirement().addList("refreshBearerAuth"),
                ),
            )
    }

    @Bean
    fun adminApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**", "/api/v4/admin/**")
            .addOperationCustomizer(operationCustomizer())
            .build()

    @Bean
    fun publicApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("public")
            .pathsToExclude("/api/v1/admin/**", "/api/v4/admin/**")
            .addOperationCustomizer(operationCustomizer())
            .build()

    @Bean
    fun operationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val apiErrorCodeExample = findAnnotation(handlerMethod, ApiErrorCodeExample::class.java)
            if (apiErrorCodeExample != null) {
                apiErrorCodeExample.value.forEach { type ->
                    generateErrorCodeResponseExample(operation.responses, type.java)
                }
            }

            operation
        }

    private fun generateErrorCodeResponseExample(
        responses: ApiResponses,
        type: Class<out ErrorCodeInterface>,
    ) {
        val errorCodes = type.enumConstants ?: return

        val statusWithExampleHolders =
            errorCodes
                .map { errorCode ->
                    val enumName = (errorCode as Enum<*>).name
                    val description = runCatching { errorCode.getExplainError() }.getOrDefault(errorCode.getMessage())

                    ExampleHolder(
                        holder = getSwaggerExample(description, errorCode),
                        code = errorCode.getStatus().value(),
                        name = "[$enumName] ${errorCode.getMessage()}",
                    )
                }.groupBy { it.code }

        addExamplesToResponses(responses, statusWithExampleHolders)
    }

    private fun getSwaggerExample(
        description: String,
        errorCode: ErrorCodeInterface,
    ): Example {
        val errorResponse = CommonResponse.Companion.createFailure(errorCode.getCode(), errorCode.getMessage())
        return Example()
            .description(description)
            .value(errorResponse)
    }

    private fun addExamplesToResponses(
        responses: ApiResponses,
        statusWithExampleHolders: Map<Int, List<ExampleHolder>>,
    ) {
        statusWithExampleHolders.forEach { (status, exampleHolders) ->
            val apiResponse = responses.computeIfAbsent(status.toString()) { ApiResponse() }
            val mediaType = getOrCreateMediaType(apiResponse)
            exampleHolders.forEach { holder -> mediaType.addExamples(holder.name, holder.holder) }
        }
    }

    private fun <A : Annotation> findAnnotation(
        handlerMethod: HandlerMethod,
        annotationType: Class<A>,
    ): A? {
        val annotation = handlerMethod.getMethodAnnotation(annotationType)
        if (annotation != null) {
            return annotation
        }
        return handlerMethod.beanType.getAnnotation(annotationType)
    }

    private fun getOrCreateMediaType(apiResponse: ApiResponse): MediaType {
        val content = apiResponse.content ?: Content().also { apiResponse.content = it }
        return content["application/json"] ?: MediaType().also { content.addMediaType("application/json", it) }
    }

    private fun getAccessSecurityScheme(): SecurityScheme =
        SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name(jwtProperties.access.header)

    private fun getRefreshSecurityScheme(): SecurityScheme =
        SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name(jwtProperties.refresh.header)
}
