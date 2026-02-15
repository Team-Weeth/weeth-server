package com.weeth.global.common.exception;

import lombok.Getter;

@Getter
public abstract class BusinessLogicException extends RuntimeException {

    private final int statusCode;
    private final ErrorCodeInterface errorCode;

    public BusinessLogicException(int code, String message) {
        super(message);
        this.statusCode = code;
        this.errorCode = null;
    }

    public BusinessLogicException(ErrorCodeInterface errorCode) {
        super(errorCode.getMessage());
        this.statusCode = errorCode.getStatus().value();
        this.errorCode = errorCode;
    }
}
