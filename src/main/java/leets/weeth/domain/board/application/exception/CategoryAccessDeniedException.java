package leets.weeth.domain.board.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class CategoryAccessDeniedException extends BusinessLogicException {
  public CategoryAccessDeniedException() {
    super(BoardErrorCode.CATEGORY_ACCESS_DENIED);
  }
}
