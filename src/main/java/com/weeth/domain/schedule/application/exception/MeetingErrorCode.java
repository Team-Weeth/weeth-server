package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 정기모임 ID에 해당하는 정기모임이 존재하지 않을 때 발생합니다.")
    MEETING_NOT_FOUND(2701, HttpStatus.NOT_FOUND, "존재하지 않는 정기모임입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
