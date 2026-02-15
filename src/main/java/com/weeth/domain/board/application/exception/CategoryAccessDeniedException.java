package com.weeth.domain.board.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class CategoryAccessDeniedException extends BusinessLogicException {
  public CategoryAccessDeniedException() {
    super(BoardErrorCode.CATEGORY_ACCESS_DENIED);
  }
}
