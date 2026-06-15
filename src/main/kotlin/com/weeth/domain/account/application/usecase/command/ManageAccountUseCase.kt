package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountUseCase(
    private val accountRepository: AccountRepository,
    private val cardinalReader: CardinalReader,
    private val clubReader: ClubReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun updateMemberVisibility(
        clubId: Long,
        accountId: Long,
        visible: Boolean,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = getAccountWithLock(clubId, accountId)

        if (visible) {
            account.showToMembers()
        } else {
            account.hideFromMembers()
        }

        account.markModifiedBy(userId)
    }

    @Transactional
    fun save(
        clubId: Long,
        request: AccountSaveRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        if (accountRepository.existsByClubIdAndCardinal(clubId, request.cardinal)) throw AccountExistsException()

        cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal)
            ?: throw CardinalNotFoundException()

        accountRepository.save(Account.create(club, request.description, request.totalAmount, request.cardinal))
    }

    private fun getAccountWithLock(
        clubId: Long,
        accountId: Long,
    ): Account {
        val account = accountRepository.findByIdWithLock(accountId) ?: throw AccountNotFoundException()
        account.validateOwnedBy(clubId)
        return account
    }
}
