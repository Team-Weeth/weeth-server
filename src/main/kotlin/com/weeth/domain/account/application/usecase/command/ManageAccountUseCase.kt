package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.exception.ClubNotFoundException
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountUseCase(
    private val accountRepository: AccountRepository,
    private val cardinalReader: CardinalReader,
    private val clubReader: ClubReader,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun save(
        clubId: Long,
        request: AccountSaveRequest,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        if (accountRepository.existsByClubIdAndCardinal(clubId, request.cardinal)) throw AccountExistsException()

        cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal)
            ?: throw CardinalNotFoundException()

        accountRepository.save(Account.create(club, request.description, request.totalAmount, request.cardinal))
    }
}
