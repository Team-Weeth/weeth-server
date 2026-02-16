package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 일정 ID에 해당하는 일정이 존재하지 않을 때 발생합니다.")
    EVENT_NOT_FOUND(2700, HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
