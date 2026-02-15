package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.ErrorCodeInterface;
import com.weeth.global.common.exception.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AccountErrorCode implements ErrorCodeInterface {

    @ExplainError("요청한 회비 장부 ID가 존재하지 않을 때 발생합니다.")
    ACCOUNT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 장부입니다."),

    @ExplainError("이미 존재하는 장부를 중복 생성하려고 할 때 발생합니다.")
    ACCOUNT_EXISTS(400, HttpStatus.BAD_REQUEST, "이미 생성된 장부입니다."),

    @ExplainError("요청한 영수증 내역이 존재하지 않을 때 발생합니다.")
    RECEIPT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 내역입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
