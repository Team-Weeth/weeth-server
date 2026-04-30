package com.weeth.global.common.web

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description = "Base62 인코딩 TSID",
    example = "1zA9",
    required = true,
    `in` = ParameterIn.PATH,
    schema = Schema(type = "string"),
)
annotation class TsidParam
