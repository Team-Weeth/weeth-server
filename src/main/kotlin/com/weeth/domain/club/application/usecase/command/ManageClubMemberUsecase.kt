package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberProfileRequest
import com.weeth.domain.club.application.exception.AlreadyJoinedException
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.application.exception.ClubCantJoinException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
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
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val fileRepository: FileRepository,
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    /**
     * 초대 코드가 일치하면 자동으로 활성 상태로 가입됨
     * MVP에서는 단일 동아리 지원만 가능
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

    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateMemberProfileRequest,
    ) {
        val members = clubMemberRepository.findActiveByUserId(userId)

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

        fileRepository
            .findAllByOwnerTypeAndOwnerIdAndStatus(
                FileOwnerType.CLUB_MEMBER_PROFILE,
                userId,
                FileStatus.UPLOADED,
            ).forEach { it.markDeleted() }

        members.forEach { it.removeProfileImage() }
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
