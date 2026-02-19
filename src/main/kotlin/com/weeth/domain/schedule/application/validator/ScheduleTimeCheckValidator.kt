package com.weeth.domain.schedule.application.validator

import com.weeth.domain.schedule.application.annotation.ScheduleTimeCheck
import com.weeth.domain.schedule.application.dto.request.ScheduleTimeRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ScheduleTimeCheckValidator : ConstraintValidator<ScheduleTimeCheck, ScheduleTimeRequest> {
    override fun isValid(
        time: ScheduleTimeRequest,
        context: ConstraintValidatorContext,
    ): Boolean = time.start.isBefore(time.end.plusMinutes(1))
}
