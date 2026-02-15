package com.weeth.global.common.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final int statusCode;
    private final ErrorCodeInterface errorCode;

    public BaseException(int code, String message) {
        super(message);
        this.statusCode = code;
        this.errorCode = null;
    }

    public BaseException(ErrorCodeInterface errorCode) {
        super(errorCode.getMessage());
        this.statusCode = errorCode.getStatus().value();
        this.errorCode = errorCode;
    }
}
