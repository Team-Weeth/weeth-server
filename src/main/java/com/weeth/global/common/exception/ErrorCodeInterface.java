package com.weeth.global.common.exception;

import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Objects;

public interface ErrorCodeInterface {
    int getCode();
    HttpStatus getStatus();
    String getMessage();

    // ExplainError 어노테이션에 작성된 설명을 조회하는 메서드
    default String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(((Enum<?>) this).name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : getMessage();
    }
}
