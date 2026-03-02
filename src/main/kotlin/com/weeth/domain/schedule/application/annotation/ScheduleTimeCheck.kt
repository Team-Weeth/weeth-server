package com.weeth.domain.schedule.application.annotation

import com.weeth.domain.schedule.application.validator.ScheduleTimeCheckValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Todo: 사용처 있는지 확인하고 없으면 제거
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ScheduleTimeCheckValidator::class])
annotation class ScheduleTimeCheck(
    val message: String = "마감 시간이 시작 시간보다 빠를 수 없습니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
