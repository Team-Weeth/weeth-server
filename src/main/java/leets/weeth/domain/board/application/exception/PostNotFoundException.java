package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class PostNotFoundException extends BusinessLogicException {
    public PostNotFoundException() {
        super(PostErrorCode.POST_NOT_FOUND);
    }
}
