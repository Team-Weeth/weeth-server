package leets.weeth.global.auth.jwt.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class TokenNotFoundException extends BusinessLogicException {
    public TokenNotFoundException() {
        super(JwtErrorCode.TOKEN_NOT_FOUND);
    }
}
