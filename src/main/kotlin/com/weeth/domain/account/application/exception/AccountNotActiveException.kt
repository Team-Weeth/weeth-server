package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountNotActiveException : BaseException(AccountErrorCode.ACCOUNT_NOT_ACTIVE)
