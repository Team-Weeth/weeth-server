package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.ClubMemberCardinalSetRequest
import com.weeth.domain.club.application.exception.AlreadyJoinedException
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.application.exception.CardinalAlreadySetException
import com.weeth.domain.club.application.exception.ClubCantJoinException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 가입, 탈퇴 UseCase.
 */
@Service
class ManageClubMemberUsecase(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val cardinalReader: CardinalReader,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    /**
     * 초대 코드가 일치하면 자동으로 활성 상태로 가입됨
     * MVP에서는 단일 동아리 지원만 가능
     */
    @Transactional
    fun join(
        clubId: Long,
        userId: Long,
        request: ClubJoinRequest,
    ) {
        val club = clubRepository.getClubById(clubId)
        val user =
            userReader.getByIdWithLock(userId)

        clubMemberRepository.findByClubIdAndUserId(clubId, userId)?.let {
            throw AlreadyJoinedException()
        }

        val isJoinedAnotherClub =
            clubMemberRepository
                .findAllByUserId(userId)
                .any { it.club.id != clubId && it.isActive() }

        if (isJoinedAnotherClub) {
            throw ClubCantJoinException()
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
     * 활동 기수를 최초 1회 설정
     * 이미 설정된 경우 CardinalAlreadySetException 발생
     */
    @Transactional
    fun setInitialCardinals(
        clubId: Long,
        userId: Long,
        request: ClubMemberCardinalSetRequest,
    ) {
        val member = clubMemberPolicy.getActiveMemberWithLock(clubId, userId)

        if (clubMemberCardinalRepository.findAllByClubMember(member).isNotEmpty()) {
            throw CardinalAlreadySetException()
        }

        val cardinals =
            request.cardinals.distinct().map { number ->
                cardinalReader.findByClubIdAndCardinalNumber(clubId, number)
                    ?: throw CardinalNotFoundException()
            }

        clubMemberCardinalRepository.saveAll(cardinals.map { ClubMemberCardinal.create(member, it) })

        cardinals.forEach { initializeAttendances(clubId, member, it) }
    }

    private fun initializeAttendances(
        clubId: Long,
        member: ClubMember,
        cardinal: Cardinal,
    ) {
        val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, listOf(cardinal.cardinalNumber))
        if (sessions.isEmpty()) return

        val attendances = sessions.map { Attendance.create(session = it, clubMember = member) }
        attendanceRepository.saveAll(attendances)
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
