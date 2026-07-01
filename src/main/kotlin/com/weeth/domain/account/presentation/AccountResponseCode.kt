package com.weeth.domain.account.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class AccountResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    ACCOUNT_DRAFT_SAVE_SUCCESS(10105, HttpStatus.OK, "회비 초안이 성공적으로 저장되었습니다."),
    ACCOUNT_UPDATE_SUCCESS(10106, HttpStatus.OK, "회비 설정이 성공적으로 수정되었습니다."),
    ACCOUNT_DRAFT_DELETE_SUCCESS(10107, HttpStatus.OK, "회비 초안이 성공적으로 폐기되었습니다."),
    ACCOUNT_PAYMENT_TARGET_FIND_SUCCESS(10108, HttpStatus.OK, "납부 대상이 성공적으로 조회되었습니다."),
    ACCOUNT_PAYMENT_TARGET_UPDATE_SUCCESS(10110, HttpStatus.OK, "납부 대상이 성공적으로 저장되었습니다."),
    ACCOUNT_REGISTRATION_COMPLETE_SUCCESS(10112, HttpStatus.OK, "회비 등록이 완료되었습니다."),
    ACCOUNT_REGISTRATION_STATUS_FIND_SUCCESS(10117, HttpStatus.OK, "회비 등록 현황이 성공적으로 조회되었습니다."),
    ACCOUNT_CARRY_OVER_SOURCE_FIND_SUCCESS(10118, HttpStatus.OK, "이월 재원 정보가 성공적으로 조회되었습니다."),
    ACCOUNT_DASHBOARD_FIND_SUCCESS(10119, HttpStatus.OK, "회비 대시보드가 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_SAVE_SUCCESS(10120, HttpStatus.OK, "거래 내역이 성공적으로 저장되었습니다."),
    ACCOUNT_TRANSACTION_FIND_SUCCESS(10121, HttpStatus.OK, "거래 내역이 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_DETAIL_FIND_SUCCESS(10122, HttpStatus.OK, "거래 내역 상세가 성공적으로 조회되었습니다."),
    ACCOUNT_TRANSACTION_UPDATE_SUCCESS(10123, HttpStatus.OK, "거래 내역이 성공적으로 수정되었습니다."),
    ACCOUNT_TRANSACTION_DELETE_SUCCESS(10124, HttpStatus.OK, "거래 내역이 성공적으로 삭제되었습니다."),
    ACCOUNT_PAYMENT_MARK_PAID_SUCCESS(10125, HttpStatus.OK, "납부가 완료 처리되었습니다."),
    ACCOUNT_PAYMENT_MARK_UNPAID_SUCCESS(10126, HttpStatus.OK, "납부가 취소 처리되었습니다."),
    ACCOUNT_PAYMENT_REFUND_SUCCESS(10127, HttpStatus.OK, "환불이 처리되었습니다."),
    ACCOUNT_PAYMENT_STATUS_FIND_SUCCESS(10128, HttpStatus.OK, "부원별 납부현황이 성공적으로 조회되었습니다."),
    ACCOUNT_MY_SUMMARY_FIND_SUCCESS(10129, HttpStatus.OK, "나의 회비 정보가 성공적으로 조회되었습니다."),
    ACCOUNT_CARDINAL_LIST_FIND_SUCCESS(10130, HttpStatus.OK, "회비 기수 목록이 성공적으로 조회되었습니다."),
}
