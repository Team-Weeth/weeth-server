package com.weeth.domain.account.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class AccountResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    ACCOUNT_SAVE_SUCCESS(10100, HttpStatus.OK, "회비가 성공적으로 저장되었습니다."),
    ACCOUNT_FIND_SUCCESS(10101, HttpStatus.OK, "회비가 성공적으로 조회되었습니다."),
    RECEIPT_SAVE_SUCCESS(10102, HttpStatus.OK, "영수증이 성공적으로 저장되었습니다."),
    RECEIPT_DELETE_SUCCESS(10103, HttpStatus.OK, "영수증이 성공적으로 삭제되었습니다."),
    RECEIPT_UPDATE_SUCCESS(10104, HttpStatus.OK, "영수증이 성공적으로 업데이트 되었습니다."),
    ACCOUNT_DRAFT_SAVE_SUCCESS(10105, HttpStatus.OK, "회비 초안이 성공적으로 저장되었습니다."),
    ACCOUNT_UPDATE_SUCCESS(10106, HttpStatus.OK, "회비 설정이 성공적으로 수정되었습니다."),
    ACCOUNT_DRAFT_DELETE_SUCCESS(10107, HttpStatus.OK, "회비 초안이 성공적으로 폐기되었습니다."),
    ACCOUNT_PAYMENT_TARGET_FIND_SUCCESS(10108, HttpStatus.OK, "납부 대상이 성공적으로 조회되었습니다."),
    ACCOUNT_PAYMENT_TARGET_UPDATE_SUCCESS(10110, HttpStatus.OK, "납부 대상이 성공적으로 저장되었습니다."),
    ACCOUNT_REGISTRATION_COMPLETE_SUCCESS(10112, HttpStatus.OK, "회비 등록이 완료되었습니다."),
    ACCOUNT_REGISTRATION_STATUS_FIND_SUCCESS(10117, HttpStatus.OK, "회비 등록 현황이 성공적으로 조회되었습니다."),
    ACCOUNT_CARRY_OVER_SOURCE_FIND_SUCCESS(10118, HttpStatus.OK, "이월 재원 정보가 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_SAVE_SUCCESS(10120, HttpStatus.OK, "거래 내역이 성공적으로 저장되었습니다."),
    ACCOUNT_TRANSACTION_FIND_SUCCESS(10121, HttpStatus.OK, "거래 내역이 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS(10122, HttpStatus.OK, "거래 내역 상세가 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_UPDATE_SUCCESS(10123, HttpStatus.OK, "거래 내역이 성공적으로 수정되었습니다."),
    ACCOUNT_TRANSACTION_DELETE_SUCCESS(10124, HttpStatus.OK, "거래 내역이 성공적으로 삭제되었습니다."),
}
