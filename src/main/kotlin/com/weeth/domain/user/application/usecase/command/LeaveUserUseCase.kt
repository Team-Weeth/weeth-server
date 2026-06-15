package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubActivityDeletionPolicy
import com.weeth.domain.user.application.exception.UserHasLeadClubException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.jwt.domain.port.AccessTokenBlacklistStorePort
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDateTime

@Service
class LeaveUserUseCase(
    private val userReader: UserReader,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubActivityDeletionPolicy: ClubActivityDeletionPolicy,
    private val jwtManageUseCase: JwtManageUseCase,
    private val accessTokenBlacklistStore: AccessTokenBlacklistStorePort,
    private val meterRegistry: MeterRegistry,
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
        revokeTokensAfterCommit(userId)
    }

    private fun revokeTokensAfterCommit(userId: Long) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    retryTokenRevoke("refresh token 삭제", "refresh_token_delete", userId) {
                        jwtManageUseCase.deleteRefreshToken(userId)
                    }
                    retryTokenRevoke("access token blacklist 등록", "access_token_blacklist", userId) {
                        accessTokenBlacklistStore.blacklist(userId)
                    }
                }
            },
        )
    }

    private fun retryTokenRevoke(
        actionName: String,
        metricAction: String,
        userId: Long,
        action: () -> Unit,
    ) {
        for (attempt in 1..TOKEN_REVOKE_ATTEMPTS) {
            val result = runCatching(action)
            if (result.isSuccess) break

            result.onFailure { exception ->
                if (attempt == TOKEN_REVOKE_ATTEMPTS) {
                    log.error(
                        "탈퇴 후 {} 최종 실패. userId={}, attempts={}",
                        actionName,
                        userId,
                        TOKEN_REVOKE_ATTEMPTS,
                        exception,
                    )
                    meterRegistry
                        .counter(TOKEN_REVOKE_FAILURE_METRIC, "action", metricAction)
                        .increment()
                } else {
                    log.warn(
                        "탈퇴 후 {} 실패. userId={}, attempt={}/{}",
                        actionName,
                        userId,
                        attempt,
                        TOKEN_REVOKE_ATTEMPTS,
                        exception,
                    )
                }
            }
        }
    }

    companion object {
        private const val TOKEN_REVOKE_ATTEMPTS = 3
        private const val TOKEN_REVOKE_FAILURE_METRIC = "user.leave.token_revoke.failure"
    }
}
