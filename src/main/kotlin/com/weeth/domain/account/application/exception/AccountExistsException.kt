package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountExistsException : BaseException(AccountErrorCode.ACCOUNT_EXISTS)
