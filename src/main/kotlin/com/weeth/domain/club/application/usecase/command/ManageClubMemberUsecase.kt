package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.ClubMemberCardinalSetRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberProfileRequest
import com.weeth.domain.club.application.exception.AlreadyJoinedException
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.application.exception.CardinalAlreadySetException
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
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
    private val fileRepository: FileRepository,
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    /**
     * 초대 코드가 일치하면 자동으로 활성 상태로 가입됨
     * 역할(LEAD/USER)별 가입 제한 정책 적용
     * TODO: 출석 초기화
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

        clubMemberPolicy.validateJoinLimit(userId)

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

    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateMemberProfileRequest,
    ) {
        val members = clubMemberRepository.findActiveByUserId(userId)
        if (members.isEmpty()) throw ClubMemberNotFoundException()

        request.profileImage?.let { profileImage ->
            fileRepository
                .findAllByOwnerTypeAndOwnerIdAndStatus(
                    FileOwnerType.CLUB_MEMBER_PROFILE,
                    userId,
                    FileStatus.UPLOADED,
                ).forEach { it.markDeleted() }

            val file =
                File.createUploaded(
                    fileName = profileImage.fileName,
                    storageKey = profileImage.storageKey,
                    fileSize = profileImage.fileSize,
                    contentType = profileImage.contentType,
                    ownerType = FileOwnerType.CLUB_MEMBER_PROFILE,
                    ownerId = userId,
                )
            fileRepository.save(file)

            val resolvedUrl = fileAccessUrlPort.resolve(file.storageKey.value)
            members.forEach { it.updateProfileImageUrl(resolvedUrl) }
        }

        request.bio?.let { bio -> members.forEach { it.updateBio(bio) } }
    }

    @Transactional
    fun deleteProfileImage(userId: Long) {
        val members = clubMemberRepository.findActiveByUserId(userId)
        if (members.isEmpty()) throw ClubMemberNotFoundException()

        fileRepository
            .findAllByOwnerTypeAndOwnerIdAndStatus(
                FileOwnerType.CLUB_MEMBER_PROFILE,
                userId,
                FileStatus.UPLOADED,
            ).forEach { it.markDeleted() }

        members.forEach { it.removeProfileImage() }
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

        if (clubMemberCardinalRepository.existsByClubMember(member)) {
            throw CardinalAlreadySetException()
        }

        val cardinals =
            request.cardinals.distinct().map { number ->
                cardinalReader.findByClubIdAndCardinalNumber(clubId, number)
                    ?: throw CardinalNotFoundException()
            }

        clubMemberCardinalRepository.saveAll(cardinals.map { ClubMemberCardinal.create(member, it) })

        initializeAttendances(clubId, member, cardinals)
    }

    // TODO: AdminClubMemberUseCase.initializeAttendances와 중복 — MVP 후 공통 서비스로 추출
    private fun initializeAttendances(
        clubId: Long,
        member: ClubMember,
        cardinals: List<Cardinal>,
    ) {
        val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, cardinals.map { it.cardinalNumber })
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
        val member = clubMemberPolicy.getActiveMemberWithLock(clubId, userId)

        if (member.memberRole == MemberRole.LEAD) {
            throw CannotLeaveAsLeadException()
        }

        member.leave()
    }
}
