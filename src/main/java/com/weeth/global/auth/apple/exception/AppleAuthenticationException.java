package com.weeth.global.auth.apple.exception;

import com.weeth.global.common.exception.BaseException;

public class AppleAuthenticationException extends BaseException {
    public AppleAuthenticationException() {
        super(401, "애플 로그인에 실패했습니다.");
    }
}
