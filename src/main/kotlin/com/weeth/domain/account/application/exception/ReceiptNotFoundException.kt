package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class ReceiptNotFoundException : BaseException(AccountErrorCode.RECEIPT_NOT_FOUND)
