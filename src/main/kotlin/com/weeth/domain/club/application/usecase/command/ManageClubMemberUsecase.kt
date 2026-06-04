package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.service.AttendanceInitializer
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
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
import com.weeth.domain.club.domain.service.ClubJoinPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 동아리 가입, 탈퇴 UseCase.
 */
@Service
class ManageClubMemberUsecase(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val cardinalReader: CardinalReader,
    private val attendanceInitializer: AttendanceInitializer,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubJoinPolicy: ClubJoinPolicy,
    private val fileRepository: FileRepository,
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    /**
     * 초대 코드가 일치하면 자동으로 활성 상태로 가입됨
     * 역할(LEAD/USER)별 가입 제한 정책 적용
     * 출석 초기화는 setInitialCardinals() 호출 시 처리됨
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

        clubJoinPolicy.validateJoinLimit(userId)

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
            val existingFiles =
                fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                    FileOwnerType.CLUB_MEMBER_PROFILE,
                    userId,
                    FileStatus.UPLOADED,
                )
            if (existingFiles.isNotEmpty()) {
                fileRepository.deleteAll(existingFiles)
            }

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

            members.forEach { it.updateProfileImageUrl(file.storageKey.value) }
        }

        request.bio?.let { bio -> members.forEach { it.updateBio(bio) } }
    }

    @Transactional
    fun deleteProfileImage(userId: Long) {
        val members = clubMemberRepository.findActiveByUserId(userId)
        if (members.isEmpty()) throw ClubMemberNotFoundException()

        val existingFiles =
            fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                FileOwnerType.CLUB_MEMBER_PROFILE,
                userId,
                FileStatus.UPLOADED,
            )
        if (existingFiles.isNotEmpty()) {
            fileRepository.deleteAll(existingFiles)
        }

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

        attendanceInitializer.initializeForMemberCardinals(clubId, member, cardinals)
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

        member.leave(LocalDateTime.now())
    }
}
