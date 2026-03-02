package com.weeth.domain.schedule.application.validator

import com.weeth.domain.schedule.application.annotation.ScheduleTimeCheck
import com.weeth.domain.schedule.application.dto.request.ScheduleTimeRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Todo: 사용처 있는지 확인하고 없으면 제거
 */
class ScheduleTimeCheckValidator : ConstraintValidator<ScheduleTimeCheck, ScheduleTimeRequest> {
    override fun isValid(
        time: ScheduleTimeRequest?,
        context: ConstraintValidatorContext,
    ): Boolean = time == null || time.start.isBefore(time.end.plusMinutes(1))
}
