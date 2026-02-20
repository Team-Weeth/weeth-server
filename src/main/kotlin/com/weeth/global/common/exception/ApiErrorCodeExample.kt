package com.weeth.global.common.exception

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeExample(
    vararg val value: KClass<out ErrorCodeInterface>,
)
