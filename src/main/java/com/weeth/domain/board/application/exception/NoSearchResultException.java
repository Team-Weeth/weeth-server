package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class NoSearchResultException extends BusinessLogicException {
	public NoSearchResultException() {
		super(BoardErrorCode.NO_SEARCH_RESULT);
	}
}
