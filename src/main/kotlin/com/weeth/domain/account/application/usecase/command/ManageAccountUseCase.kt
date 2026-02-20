package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountUseCase(
    private val accountRepository: AccountRepository,
    private val cardinalGetService: CardinalGetService,
) {
    @Transactional
    fun save(request: AccountSaveRequest) {
        if (accountRepository.existsByCardinal(request.cardinal)) throw AccountExistsException()
        cardinalGetService.findByAdminSide(request.cardinal)
        accountRepository.save(Account.create(request.description, request.totalAmount, request.cardinal))
    }
}
