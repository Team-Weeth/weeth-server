package com.weeth.global.config.swagger;

import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExampleHolder;
import com.weeth.global.common.response.CommonResponse;
import com.weeth.global.config.properties.JwtProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Configuration
@RequiredArgsConstructor
@OpenAPIDefinition(
        info = @Info(
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
                        """
        )
)
public class SwaggerConfig {

    private final JwtProperties jwtProperties;

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme accessSecurityScheme = getAccessSecurityScheme();
        SecurityScheme refreshSecurityScheme = getRefreshSecurityScheme();

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", accessSecurityScheme)
                        .addSecuritySchemes("refreshBearerAuth", refreshSecurityScheme))
                .security(List.of(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("refreshBearerAuth")
                ));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodeExample apiErrorCodeExample = findAnnotation(handlerMethod, ApiErrorCodeExample.class);
            if (apiErrorCodeExample != null) {
                for (Class<? extends ErrorCodeInterface> type : apiErrorCodeExample.value()) {
                    generateErrorCodeResponseExample(operation.getResponses(), type);
                }
            }

            return operation;
        };
    }

    private void generateErrorCodeResponseExample(ApiResponses responses, Class<? extends ErrorCodeInterface> type) {
        ErrorCodeInterface[] errorCodes = type.getEnumConstants();

        Map<Integer, List<ExampleHolder>> statusWithExampleHolders =
                Arrays.stream(errorCodes)
                        .map(errorCode -> {
                            try {
                                String enumName = ((Enum<?>) errorCode).name();

                                return ExampleHolder.builder()
                                        .holder(getSwaggerExample(errorCode.getExplainError(), errorCode))
                                        .code(errorCode.getStatus().value())
                                        .name("[" + enumName + "] " + errorCode.getMessage())
                                        .build();
                            } catch (NoSuchFieldException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(groupingBy(ExampleHolder::getCode));

        addExamplesToResponses(responses, statusWithExampleHolders);
    }

    private Example getSwaggerExample(String description, ErrorCodeInterface errorCode) {
        CommonResponse<Void> errorResponse = CommonResponse.createFailure(errorCode.getCode(), errorCode.getMessage());
        Example example = new Example();
        example.description(description);
        example.setValue(errorResponse);

        return example;
    }

    private void addExamplesToResponses(ApiResponses responses, Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
        statusWithExampleHolders.forEach((status, exampleHolders) -> {
            ApiResponse apiResponse = responses.computeIfAbsent(String.valueOf(status), k -> new ApiResponse());
            MediaType mediaType = getOrCreateMediaType(apiResponse);
            exampleHolders.forEach(holder -> mediaType.addExamples(holder.getName(), holder.getHolder()));
        });
    }

    private <A extends java.lang.annotation.Annotation> A findAnnotation(org.springframework.web.method.HandlerMethod handlerMethod, Class<A> annotationType) {
        A annotation = handlerMethod.getMethodAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(annotationType);
    }

    private MediaType getOrCreateMediaType(ApiResponse apiResponse) {
        Content content = apiResponse.getContent();
        if (content == null) {
            content = new Content();
            apiResponse.setContent(content);
        }

        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType("application/json", mediaType);
        }

        return mediaType;
    }

    private SecurityScheme getAccessSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(jwtProperties.getAccess().getHeader());
    }

    private SecurityScheme getRefreshSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(jwtProperties.getRefresh().getHeader());
    }
}
