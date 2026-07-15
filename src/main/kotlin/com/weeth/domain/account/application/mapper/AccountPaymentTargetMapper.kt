package com.weeth.domain.account.application.mapper

import com.weeth.domain.account.application.dto.response.AccountPaymentTargetResponse
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import org.springframework.stereotype.Component

@Component
class AccountPaymentTargetMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toResponse(target: AccountPaymentTarget): AccountPaymentTargetResponse =
        toResponse(clubMember = target.clubMember, target = target)

    fun toResponse(
        clubMember: ClubMember,
        target: AccountPaymentTarget?,
    ): AccountPaymentTargetResponse =
        AccountPaymentTargetResponse(
            targetId = target?.id,
            paymentTargetInfo = toPaymentTargetInfo(clubMember),
            targetStatus = target?.targetStatus ?: AccountTargetStatus.EXCLUDED,
            paymentStatus = target?.paymentStatus ?: AccountPaymentStatus.UNPAID,
            dueAmount = target?.dueAmount ?: 0,
            paidAmount = target?.paidAmount ?: 0,
            paidAt = target?.paidAt,
            confirmedBy = target?.confirmedBy,
            memo = target?.memo,
        )

    private fun toPaymentTargetInfo(clubMember: ClubMember) =
        AccountPaymentTargetResponse.PaymentTargetInfoResponse(
            userId = clubMember.user.id,
            clubMemberId = clubMember.id,
            name = clubMember.user.name,
            tel = clubMember.user.telValue,
            school = clubMember.user.school,
            department = clubMember.user.department,
            memberRole = clubMember.memberRole,
            memberStatus = clubMember.memberStatus,
            profileImageUrl = resolveClubImage(clubMember.profileImageStorageKey),
        )

    private fun resolveClubImage(storageKey: String?): String? = storageKey?.let { fileAccessUrlPort.resolve(it) }
}
