package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountTransactionNotFoundException : BaseException(AccountErrorCode.ACCOUNT_TRANSACTION_NOT_FOUND)
