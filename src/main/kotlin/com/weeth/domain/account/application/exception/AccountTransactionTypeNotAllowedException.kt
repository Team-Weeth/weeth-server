package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountTransactionTypeNotAllowedException : BaseException(AccountErrorCode.ACCOUNT_TRANSACTION_TYPE_NOT_ALLOWED)
