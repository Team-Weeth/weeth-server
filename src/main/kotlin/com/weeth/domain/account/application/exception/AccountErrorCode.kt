package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class AccountErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 회비 장부 ID가 존재하지 않을 때 발생합니다.")
    ACCOUNT_NOT_FOUND(2100, HttpStatus.NOT_FOUND, "존재하지 않는 장부입니다."),

    @ExplainError("이미 존재하는 장부를 중복 생성하려고 할 때 발생합니다.")
    ACCOUNT_EXISTS(2101, HttpStatus.BAD_REQUEST, "이미 생성된 장부입니다."),

    @ExplainError("요청한 영수증 내역이 존재하지 않을 때 발생합니다.")
    RECEIPT_NOT_FOUND(2102, HttpStatus.NOT_FOUND, "존재하지 않는 내역입니다."),

    @ExplainError("영수증이 요청한 기수의 장부에 속하지 않을 때 발생합니다.")
    RECEIPT_ACCOUNT_MISMATCH(2103, HttpStatus.BAD_REQUEST, "영수증이 해당 기수의 장부에 속하지 않습니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
