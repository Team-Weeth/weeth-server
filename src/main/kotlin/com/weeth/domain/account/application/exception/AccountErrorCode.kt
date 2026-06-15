package com.weeth.domain.account.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class AccountErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 회비 장부 ID가 존재하지 않을 때 발생합니다.")
    ACCOUNT_NOT_FOUND(20100, HttpStatus.NOT_FOUND, "존재하지 않는 장부입니다."),

    @ExplainError("이미 존재하는 장부를 중복 생성하려고 할 때 발생합니다.")
    ACCOUNT_EXISTS(20101, HttpStatus.BAD_REQUEST, "이미 생성된 장부입니다."),

    @ExplainError("요청한 영수증 내역이 존재하지 않을 때 발생합니다.")
    RECEIPT_NOT_FOUND(20102, HttpStatus.NOT_FOUND, "존재하지 않는 내역입니다."),

    @ExplainError("영수증이 요청한 기수의 장부에 속하지 않거나 동아리에 속하지 않는 경우에 발생합니다.")
    RECEIPT_ACCOUNT_MISMATCH(20103, HttpStatus.BAD_REQUEST, "영수증이 해당 기수의 장부에 속하지 않습니다."),

    @ExplainError("초안 상태가 아닌 회비 장부를 등록 플로우에서 수정하거나 폐기하려고 할 때 발생합니다.")
    ACCOUNT_INVALID_DRAFT_STATE(20104, HttpStatus.BAD_REQUEST, "초안 상태의 회비 장부만 처리할 수 있습니다."),

    @ExplainError("납부 대상 멤버가 기수 명부에 없거나, 대상/제외 목록에 같은 멤버가 동시에 포함될 때 발생합니다.")
    ACCOUNT_PAYMENT_TARGET_MEMBER_INVALID(20105, HttpStatus.BAD_REQUEST, "유효하지 않은 납부 대상 멤버입니다."),

    @ExplainError("이미 납부 완료된 대상을 제외하려고 할 때 발생합니다.")
    ACCOUNT_PAYMENT_TARGET_ALREADY_PAID(20106, HttpStatus.BAD_REQUEST, "납부 완료된 대상은 제외할 수 없습니다."),

    @ExplainError("모든 등록 단계를 저장하지 않은 채 회비 등록 완료를 요청할 때 발생합니다.")
    ACCOUNT_REGISTRATION_STEP_INCOMPLETE(20107, HttpStatus.BAD_REQUEST, "모든 등록 단계를 완료한 후 등록할 수 있습니다."),

    @ExplainError("이월 금액이 완료 시점의 이전 기수 장부 잔액과 다를 때 발생합니다. 이월 재원을 다시 조회한 후 이월 설정을 재저장해야 합니다.")
    ACCOUNT_CARRY_OVER_AMOUNT_MISMATCH(20108, HttpStatus.CONFLICT, "이전 기수 잔액이 변경되었습니다. 이월 금액을 다시 확인해주세요."),

    @ExplainError("요청한 거래 내역이 존재하지 않거나 다른 장부에 속할 때 발생합니다.")
    ACCOUNT_TRANSACTION_NOT_FOUND(20109, HttpStatus.NOT_FOUND, "존재하지 않는 거래 내역입니다."),

    @ExplainError("수입/지출이 아닌 시스템 거래(납부/이월/환불)를 직접 등록·수정·삭제하려 할 때 발생합니다.")
    ACCOUNT_TRANSACTION_TYPE_NOT_ALLOWED(20111, HttpStatus.BAD_REQUEST, "직접 등록·수정할 수 없는 거래 유형입니다."),

    @ExplainError("등록이 완료되지 않은(초안 등) 장부에 운영 기능을 요청할 때 발생합니다.")
    ACCOUNT_NOT_ACTIVE(20112, HttpStatus.BAD_REQUEST, "등록이 완료된 장부에서만 운영할 수 있습니다."),
}
