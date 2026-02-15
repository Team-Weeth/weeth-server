package com.weeth.domain.schedule.application.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import com.weeth.domain.schedule.application.validator.ScheduleTimeCheckValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ScheduleTimeCheckValidator.class)
public @interface ScheduleTimeCheck {

    String message() default "마감 시간이 시작 시간보다 빠를 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
