package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class NoSearchResultException extends BaseException {
	public NoSearchResultException() {
		super(BoardErrorCode.NO_SEARCH_RESULT);
	}
}
