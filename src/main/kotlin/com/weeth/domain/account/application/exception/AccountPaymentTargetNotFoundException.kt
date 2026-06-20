package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountPaymentTargetNotFoundException : BaseException(AccountErrorCode.ACCOUNT_PAYMENT_TARGET_NOT_FOUND)
