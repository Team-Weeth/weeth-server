package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountUseCase(
    private val accountRepository: AccountRepository,
    private val cardinalReader: CardinalReader,
) {
    @Transactional
    fun save(request: AccountSaveRequest) {
        if (accountRepository.existsByCardinal(request.cardinal)) throw AccountExistsException()
        cardinalReader.getByCardinalNumber(request.cardinal)
        accountRepository.save(Account.create(request.description, request.totalAmount, request.cardinal))
    }
}
