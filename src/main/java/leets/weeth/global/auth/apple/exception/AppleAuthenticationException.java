package leets.weeth.global.auth.apple.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AppleAuthenticationException extends BusinessLogicException {
    public AppleAuthenticationException() {
        super(401, "애플 로그인에 실패했습니다.");
    }
}
