package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubActivityDeletionPolicy
import com.weeth.domain.user.application.exception.UserHasLeadClubException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDateTime

@Service
class WithdrawUserUseCase(
    private val userReader: UserReader,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubActivityDeletionPolicy: ClubActivityDeletionPolicy,
    private val jwtManageUseCase: JwtManageUseCase,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(userId: Long) {
        val now = LocalDateTime.now(clock)
        val user = userReader.getByIdWithLock(userId)
        val activeMembers = clubMemberRepository.findAllActiveByUserIdWithLock(userId)

        if (activeMembers.any { it.memberRole == MemberRole.LEAD }) {
            throw UserHasLeadClubException()
        }

        activeMembers.forEach { member ->
            clubActivityDeletionPolicy.markMemberActivitiesDeleted(member, now)
            member.leave(now)
        }

        user.leave(now)
        deleteRefreshTokenAfterCommit(userId)
    }

    private fun deleteRefreshTokenAfterCommit(userId: Long) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runCatching {
                        jwtManageUseCase.deleteRefreshToken(userId)
                    }.onFailure { e ->
                        log.warn("탈퇴 후 refresh token 삭제 실패. userId={}", userId, e)
                    }
                }
            },
        )
    }
}
