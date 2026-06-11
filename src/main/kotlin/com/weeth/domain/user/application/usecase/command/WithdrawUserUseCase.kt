package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubActivityDeletionPolicy
import com.weeth.domain.user.application.exception.UserHasLeadClubException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        jwtManageUseCase.deleteRefreshToken(userId)
    }
}
