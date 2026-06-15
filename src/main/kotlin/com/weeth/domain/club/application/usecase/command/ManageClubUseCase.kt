package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.application.dto.response.ClubCreateResponse
import com.weeth.domain.club.application.exception.DuplicateClubException
import com.weeth.domain.club.application.exception.EmailRequiredForPrimaryContactException
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubCodePolicy
import com.weeth.domain.club.domain.service.ClubJoinPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 관리 유스케이스
 * 생성은 누구나 가능하지만 그 외 작업은 관리자만 가능
 */
@Service
class ManageClubUseCase(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val cardinalRepository: CardinalRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val boardRepository: BoardRepository,
    private val userReader: UserReader,
    private val clubJoinPolicy: ClubJoinPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val fileRepository: FileRepository,
    private val clubMapper: ClubMapper,
) {
    /**
     * 새로운 동아리를 생성
     * 생성자는 자동으로 LEAD 권한 설정
     * 1기부터 currentCardinal기까지 Cardinal을 자동 생성하고, LEAD를 최신 기수에 배정
     */
    @Transactional
    fun create(
        userId: Long,
        request: ClubCreateRequest,
    ): ClubCreateResponse {
        validatePrimaryContactEmail(request.primaryContact, request.contactEmail)
        checkDuplicateClubName(request.schoolName, request.name)

        val user =
            userReader.getByIdWithLock(userId)
        if (!user.isRegistered()) throw UserInActiveException()
        clubJoinPolicy.validateCreateLimit(userId)

        val code = ClubCodePolicy.generateCode()
        val clubContact =
            ClubContact.from(
                email = request.contactEmail,
                phoneNumber = request.contactPhoneNumber,
                primaryContact = request.primaryContact,
            )

        val club =
            Club.create(
                name = request.name,
                code = code,
                schoolName = request.schoolName,
                clubContact = clubContact,
                description = request.description,
                profileImageStorageKey = request.profileImage?.storageKey,
                backgroundImageStorageKey = request.backgroundImage?.storageKey,
            )

        clubRepository.save(club)

        saveFileIfPresent(request.profileImage, FileOwnerType.CLUB_PROFILE, club.id)
        saveFileIfPresent(request.backgroundImage, FileOwnerType.CLUB_BACKGROUND, club.id)

        // 공지사항 게시판 자동 생성 (관리자만 작성 가능, displayOrder=0)
        val noticeBoard =
            Board(
                club = club,
                name = "공지사항",
                description = "운영진이 공지사항을 올리는 게시판입니다.",
                type = BoardType.NOTICE,
                config = BoardConfig(writePermission = MemberRole.ADMIN),
            )
        boardRepository.save(noticeBoard)

        val leadMember =
            ClubMember
                .create(
                    club = club,
                    user = user,
                    memberRole = MemberRole.LEAD,
                ).apply {
                    accept()
                }

        clubMemberRepository.save(leadMember)

        // 1기 - currentCardinal기까지 Cardinal 자동 생성
        val cardinals =
            (1..request.currentCardinal).map { number ->
                Cardinal.create(
                    club = club,
                    cardinalNumber = number,
                    status = if (number == request.currentCardinal) CardinalStatus.IN_PROGRESS else CardinalStatus.DONE,
                )
            }

        cardinalRepository.saveAll(cardinals)

        // LEAD 멤버를 최신 기수에 배정
        clubMemberCardinalRepository.save(ClubMemberCardinal.create(leadMember, cardinals.last()))

        return clubMapper.toCreateResponse(club)
    }

    @Transactional
    fun update(
        clubId: Long,
        userId: Long,
        request: ClubUpdateRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)

        if (request.primaryContact == PrimaryContact.EMAIL) {
            val resolvedEmail = request.contactEmail ?: club.clubContact.email
            if (resolvedEmail == null) {
                throw EmailRequiredForPrimaryContactException()
            }
        }

        request.profileImage?.let { image ->
            deleteExistingFiles(FileOwnerType.CLUB_PROFILE, clubId)
            saveFile(image, FileOwnerType.CLUB_PROFILE, clubId)
        }

        request.backgroundImage?.let { image ->
            deleteExistingFiles(FileOwnerType.CLUB_BACKGROUND, clubId)
            saveFile(image, FileOwnerType.CLUB_BACKGROUND, clubId)
        }

        club.update(
            name = request.name,
            schoolName = request.schoolName,
            description = request.description,
            contactEmail = request.contactEmail,
            contactPhoneNumber = request.contactPhoneNumber,
            primaryContact = request.primaryContact,
            profileImageStorageKey = request.profileImage?.storageKey,
            backgroundImageStorageKey = request.backgroundImage?.storageKey,
        )
    }

    @Transactional
    fun regenerateCode(
        clubId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        val newCode = ClubCodePolicy.generateCode()
        club.regenerateCode(newCode)
    }

    @Transactional
    fun deleteProfileImage(
        clubId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        deleteExistingFiles(FileOwnerType.CLUB_PROFILE, clubId)
        club.removeProfileImage()
    }

    @Transactional
    fun deleteBackgroundImage(
        clubId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club = clubRepository.getClubById(clubId)
        deleteExistingFiles(FileOwnerType.CLUB_BACKGROUND, clubId)
        club.removeBackgroundImage()
    }

    private fun saveFileIfPresent(
        request: FileSaveRequest?,
        ownerType: FileOwnerType,
        ownerId: Long,
    ) {
        request?.let { saveFile(it, ownerType, ownerId) }
    }

    private fun saveFile(
        request: FileSaveRequest,
        ownerType: FileOwnerType,
        ownerId: Long,
    ) {
        val file =
            File.createUploaded(
                fileName = request.fileName,
                storageKey = request.storageKey,
                fileSize = request.fileSize,
                contentType = request.contentType,
                ownerType = ownerType,
                ownerId = ownerId,
            )
        fileRepository.save(file)
    }

    private fun deleteExistingFiles(
        ownerType: FileOwnerType,
        ownerId: Long,
    ) {
        val files = fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(ownerType, ownerId, FileStatus.UPLOADED)

        if (files.isNotEmpty()) {
            fileRepository.deleteAll(files)
        }
    }

    private fun validatePrimaryContactEmail(
        primaryContact: PrimaryContact,
        contactEmail: String?,
    ) {
        if (primaryContact == PrimaryContact.EMAIL && contactEmail == null) {
            throw EmailRequiredForPrimaryContactException()
        }
    }

    private fun checkDuplicateClubName(
        schoolName: String,
        clubName: String,
    ) {
        if (clubRepository.existsBySchoolNameAndName(schoolName, clubName)) {
            throw DuplicateClubException()
        }
    }
}
