package leets.weeth.global.auth.jwt.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AnonymousAuthenticationException extends BusinessLogicException {
    public AnonymousAuthenticationException() {
        super(JwtErrorCode.ANONYMOUS_AUTHENTICATION);
    }
}
