package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class CategoryAccessDeniedException : BaseException(BoardErrorCode.CATEGORY_ACCESS_DENIED)
