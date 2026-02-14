package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class PageNotFoundException extends BusinessLogicException {
    public PageNotFoundException() {
        super(BoardErrorCode.PAGE_NOT_FOUND);
    }
}
