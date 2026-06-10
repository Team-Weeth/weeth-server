package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.response.CreateAccountDraftResponse
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.application.exception.AccountInvalidDraftStateException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterAccountUseCase(
    private val accountRepository: AccountRepository,
    private val cardinalReader: CardinalReader,
    private val clubReader: ClubReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val userReader: UserReader,
) {
    @Transactional
    fun createDraft(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): CreateAccountDraftResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        accountRepository.findByClubIdAndCardinal(clubId, cardinal)?.let {
            if (it.status == AccountStatus.DRAFT) {
                return CreateAccountDraftResponse(
                    accountId = it.id,
                    isNew = false,
                    lastModifiedByName =
                        it.lastModifiedBy?.let { modifierId ->
                            userReader.findByIdOrNull(modifierId)?.name
                        },
                )
            }
            throw AccountExistsException()
        }

        cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinal)
            ?: throw CardinalNotFoundException()

        val account =
            Account
                .createDraft(club = clubReader.getClubById(clubId), cardinal = cardinal)
                .also { it.markModifiedBy(userId) }

        return CreateAccountDraftResponse(
            accountId = accountRepository.save(account).id,
            isNew = true,
            lastModifiedByName = null,
        )
    }

    @Transactional
    fun discardDraft(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        if (account.status != AccountStatus.DRAFT) throw AccountInvalidDraftStateException()

        accountRepository.delete(account)
    }

    @Transactional
    fun saveBasic(
        clubId: Long,
        accountId: Long,
        request: SaveAccountBasicRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        account.updateBasicInfo(
            name = request.name,
            duesAmount = Money.of(request.duesAmount),
            description = request.description,
        )

        account.markModifiedBy(userId)
    }

    private fun getAccountWithLock(
        clubId: Long,
        accountId: Long,
    ): Account {
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        if (account.club.id != 0L && account.club.id != clubId) throw AccountNotFoundException()
        return account
    }

    private fun ClubMember.belongsTo(clubId: Long): Boolean = club.id == 0L || club.id == clubId
}
