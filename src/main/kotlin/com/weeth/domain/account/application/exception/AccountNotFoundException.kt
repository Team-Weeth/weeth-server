package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountNotFoundException : BaseException(AccountErrorCode.ACCOUNT_NOT_FOUND)
