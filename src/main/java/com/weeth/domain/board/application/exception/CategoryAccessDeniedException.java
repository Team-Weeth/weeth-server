package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BaseException;

public class CategoryAccessDeniedException extends BaseException {
  public CategoryAccessDeniedException() {
    super(BoardErrorCode.CATEGORY_ACCESS_DENIED);
  }
}
