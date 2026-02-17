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

    public BaseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = code;
        this.errorCode = null;
    }

    public BaseException(ErrorCodeInterface errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.statusCode = errorCode.getStatus().value();
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCodeInterface errorCode) {
        this(errorCode, null);
    }
}
