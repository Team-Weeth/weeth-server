package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountPaymentTargetPaidException : BaseException(AccountErrorCode.ACCOUNT_PAYMENT_TARGET_ALREADY_PAID)
