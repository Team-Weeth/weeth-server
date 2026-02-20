package com.weeth.domain.account.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class AccountResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    ACCOUNT_SAVE_SUCCESS(1100, HttpStatus.OK, "회비가 성공적으로 저장되었습니다."),
    ACCOUNT_FIND_SUCCESS(1101, HttpStatus.OK, "회비가 성공적으로 조회되었습니다."),
    RECEIPT_SAVE_SUCCESS(1102, HttpStatus.OK, "영수증이 성공적으로 저장되었습니다."),
    RECEIPT_DELETE_SUCCESS(1103, HttpStatus.OK, "영수증이 성공적으로 삭제되었습니다."),
    RECEIPT_UPDATE_SUCCESS(1104, HttpStatus.OK, "영수증이 성공적으로 업데이트 되었습니다."),
}
