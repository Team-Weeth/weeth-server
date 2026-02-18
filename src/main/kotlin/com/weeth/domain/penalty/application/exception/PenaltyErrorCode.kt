package com.weeth.domain.penalty.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PenaltyErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 패널티 ID가 존재하지 않을 때 발생합니다.")
    PENALTY_NOT_FOUND(2600, HttpStatus.NOT_FOUND, "존재하지 않는 패널티입니다."),

    @ExplainError("시스템에 의해 자동 부여된 패널티를 수동으로 삭제하려 할 때 발생합니다.")
    AUTO_PENALTY_DELETE_NOT_ALLOWED(2601, HttpStatus.BAD_REQUEST, "자동 생성된 패널티는 삭제할 수 없습니다");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
