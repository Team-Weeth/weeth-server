package com.weeth.global.common.web

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TsidPathVariable(
    val value: String = "",
)
