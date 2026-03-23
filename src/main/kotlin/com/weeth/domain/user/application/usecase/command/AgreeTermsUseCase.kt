package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.AgreeTermsRequest
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AgreeTermsUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        request: AgreeTermsRequest,
    ) {
        val user = userRepository.getById(userId)
        user.agreeTerms(request.termsAgreed, request.privacyAgreed)
    }
}
