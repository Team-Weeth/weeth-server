package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AccountPaymentTargetResponse(
    @field:Schema(description = "납부 대상 ID. 아직 선택되지 않은 후보 부원은 null")
    val targetId: Long?,
    @field:Schema(description = "동아리 부원 프로필")
    val paymentTargetInfo: PaymentTargetInfoResponse,
    @field:Schema(description = "납부 대상 상태 (TARGETED: 대상, EXCLUDED: 제외)", example = "TARGETED")
    val targetStatus: AccountTargetStatus,
    @field:Schema(description = "납부 상태 (PAID: 납부 완료, UNPAID: 미납)", example = "UNPAID")
    val paymentStatus: AccountPaymentStatus,
    @field:Schema(description = "납부 금액 (원)", example = "50000")
    val dueAmount: Int,
    @field:Schema(description = "실제 납부된 금액 (원)", example = "50000")
    val paidAmount: Int,
    @field:Schema(description = "납부 일시", nullable = true)
    val paidAt: LocalDateTime?,
    @field:Schema(description = "납부 확인자 ID", nullable = true)
    val confirmedBy: Long?,
    @field:Schema(description = "메모", nullable = true)
    val memo: String?,
) {
    data class PaymentTargetInfoResponse(
        @field:Schema(description = "사용자 ID", example = "1")
        val userId: Long,
        @field:Schema(description = "멤버 ID", example = "1")
        val clubMemberId: Long,
        @field:Schema(description = "사용자 이름", example = "홍길동")
        val name: String,
        @field:Schema(description = "전화번호", example = "01012345678")
        val tel: String?,
        @field:Schema(description = "학교", example = "가천대학교")
        val school: String?,
        @field:Schema(description = "학과", example = "컴퓨터공학과")
        val department: String?,
        @field:Schema(description = "멤버 권한", example = "USER")
        val memberRole: MemberRole,
        @field:Schema(description = "멤버 상태", example = "ACTIVE")
        val memberStatus: MemberStatus,
        @field:Schema(description = "동아리 프로필 이미지 URL", example = "https://cdn.example.com/profile.jpg")
        val profileImageUrl: String?,
    )
}
