package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.BaseException

class AccountPaymentNotRefundableException : BaseException(AccountErrorCode.ACCOUNT_PAYMENT_NOT_REFUNDABLE)
