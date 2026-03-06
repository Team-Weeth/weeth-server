package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.exception.AlreadyJoinedException
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 가입, 탈퇴 UseCase.
 */
@Service
class JoinClubUseCase(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    /**
     * 초대 코드가 일치하면 자동으로 활성 상태로 가입됨
     */
    @Transactional
    fun join(
        clubId: Long,
        userId: Long,
        request: ClubJoinRequest,
    ) {
        val club = clubRepository.getClubById(clubId)
        val user =
            userReader.getById(userId)

        clubMemberRepository.findByClubIdAndUserId(clubId, userId)?.let {
            throw AlreadyJoinedException()
        }

        ClubCodePolicy.validate(club.code, request.code)

        val member =
            ClubMember
                .create(
                    club = club,
                    user = user,
                    memberRole = MemberRole.USER,
                ).apply {
                    accept()
                }

        clubMemberRepository.save(member)
    }

    /**
     * LEAD 권한을 가진 멤버는 탈퇴 불가
     */
    @Transactional
    fun leave(
        clubId: Long,
        userId: Long,
    ) {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)

        if (member.memberRole == MemberRole.LEAD) {
            throw CannotLeaveAsLeadException()
        }

        member.leave()
    }
}
