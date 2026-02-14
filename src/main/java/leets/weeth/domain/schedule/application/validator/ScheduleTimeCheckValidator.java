package leets.weeth.domain.schedule.application.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import leets.weeth.domain.schedule.application.annotation.ScheduleTimeCheck;
import leets.weeth.domain.schedule.application.dto.ScheduleDTO.Time;

public class ScheduleTimeCheckValidator implements ConstraintValidator<ScheduleTimeCheck, Time> {

    @Override
    public void initialize(ScheduleTimeCheck constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Time time, ConstraintValidatorContext context) {
        return time.start().isBefore(time.end().plusMinutes(1));
    }
}