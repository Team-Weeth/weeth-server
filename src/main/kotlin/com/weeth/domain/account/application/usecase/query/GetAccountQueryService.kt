package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountMapper
import com.weeth.domain.account.application.mapper.ReceiptMapper
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.ReceiptRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetAccountQueryService(
    private val accountRepository: AccountRepository,
    private val receiptRepository: ReceiptRepository,
    private val fileReader: FileReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val accountMapper: AccountMapper,
    private val receiptMapper: ReceiptMapper,
    private val fileMapper: FileMapper,
) {
    fun findByCardinal(
        clubId: Long,
        userId: Long,
        cardinal: Int,
    ): AccountResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val account = accountRepository.findByClubIdAndCardinal(clubId, cardinal) ?: throw AccountNotFoundException()
        val receipts = receiptRepository.findAllByAccountIdOrderByCreatedAtDesc(account.id)
        val receiptIds = receipts.map { it.id }

        val filesByReceiptId =
            fileReader
                .findAll(FileOwnerType.RECEIPT, receiptIds, null)
                .groupBy({ it.ownerId }, { fileMapper.toFileResponse(it) })

        return accountMapper.toResponse(account, receiptMapper.toResponses(receipts, filesByReceiptId))
    }
}
