package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.dto.request.AssignClubProfileRequest
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.exception.UserProfileAssignmentNotAllowedException
import com.weeth.domain.user.application.exception.UserProfileDuplicateClubAssignmentException
import com.weeth.domain.user.application.exception.UserProfileInUseException
import com.weeth.domain.user.application.exception.UserProfileInvalidClubIdException
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageUserProfileUseCase(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val fileRepository: FileRepository,
    private val userProfileMapper: UserProfileMapper,
) {
    @Transactional
    fun create(
        userId: Long,
        request: CreateMultiProfileRequest,
    ): UserProfileResponse {
        val user = userRepository.getById(userId)
        val profile = userProfileMapper.toEntity(user, request)
        val savedProfile = userProfileRepository.save(profile)

        saveFileIfPresent(request.profileImage, FileOwnerType.USER_PROFILE_IMAGE, savedProfile.id)
        saveFileIfPresent(request.headerImage, FileOwnerType.USER_PROFILE_HEADER, savedProfile.id)
        assignCreatedProfileIfRequested(userId, savedProfile, request.clubIds)

        return userProfileMapper.toResponse(savedProfile)
    }

    @Transactional
    fun update(
        userId: Long,
        profileId: Long,
        request: UpdateMultiProfileRequest,
    ): UserProfileResponse {
        val profile =
            userProfileRepository
                .findByIdAndUserIdWithLock(profileId, userId)
                .orElseThrow { UserProfileNotFoundException() }

        profile.update(
            name = request.name,
            bio = request.bio,
        )
        updateImage(
            request = request.profileImage,
            ownerType = FileOwnerType.USER_PROFILE_IMAGE,
            ownerId = profile.id,
            updateStorageKey = { profile.update(profileImageStorageKey = it) },
        )
        updateImage(
            request = request.headerImage,
            ownerType = FileOwnerType.USER_PROFILE_HEADER,
            ownerId = profile.id,
            updateStorageKey = { profile.update(headerImageStorageKey = it) },
        )

        return userProfileMapper.toResponse(profile)
    }

    @Transactional
    fun deleteProfileImage(
        userId: Long,
        profileId: Long,
    ) {
        val profile = getOwnedProfileWithLock(userId, profileId)

        fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, profile.id)
        profile.removeProfileImage()
    }

    @Transactional
    fun deleteHeaderImage(
        userId: Long,
        profileId: Long,
    ) {
        val profile = getOwnedProfileWithLock(userId, profileId)

        fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, profile.id)
        profile.removeHeaderImage()
    }

    @Transactional
    fun assignClubProfiles(
        userId: Long,
        request: AssignClubProfileRequest,
    ) {
        val assignments =
            request.assignments.map {
                DecodedClubProfileAssignment(
                    clubId = decodeClubId(it.clubId),
                    profileId = it.profileId,
                )
            }
        validateDuplicateClubAssignments(assignments)

        val profileIds = assignments.map { it.profileId }.distinct()
        val profilesById = findOwnedProfiles(userId, profileIds)
        val clubIds = assignments.map { it.clubId }.distinct().sorted()
        val membersByClubId = findAssignableMembers(userId, clubIds)

        assignments.forEach { assignment ->
            val member = membersByClubId[assignment.clubId] ?: throw UserProfileAssignmentNotAllowedException()
            val profile = profilesById[assignment.profileId] ?: throw UserProfileNotFoundException()
            member.assignProfile(profile)
        }
    }

    @Transactional
    fun delete(
        userId: Long,
        profileId: Long,
    ) {
        val profile =
            userProfileRepository
                .findByIdAndUserIdWithLock(profileId, userId)
                .orElseThrow { UserProfileNotFoundException() }

        if (clubMemberRepository.existsByUserProfileIdAndMemberStatus(profile.id, MemberStatus.ACTIVE)) {
            throw UserProfileInUseException()
        }

        fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, profile.id)
        fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, profile.id)
        clubMemberRepository.clearUserProfileReferences(profile.id)
        userProfileRepository.delete(profile)
    }

    private fun saveFileIfPresent(
        request: FileSaveRequest?,
        ownerType: FileOwnerType,
        ownerId: Long,
    ) {
        request?.let {
            fileRepository.save(
                File.createUploaded(
                    fileName = it.fileName,
                    storageKey = it.storageKey,
                    fileSize = it.fileSize,
                    contentType = it.contentType,
                    ownerType = ownerType,
                    ownerId = ownerId,
                ),
            )
        }
    }

    private fun updateImage(
        request: FileSaveRequest?,
        ownerType: FileOwnerType,
        ownerId: Long,
        updateStorageKey: (String) -> Unit,
    ) {
        if (request != null) {
            fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(ownerType, ownerId)
            saveFileIfPresent(request, ownerType, ownerId)
            updateStorageKey(request.storageKey)
        }
    }

    private fun getOwnedProfileWithLock(
        userId: Long,
        profileId: Long,
    ): UserProfile =
        userProfileRepository
            .findByIdAndUserIdWithLock(profileId, userId)
            .orElseThrow { UserProfileNotFoundException() }

    private fun decodeClubId(clubId: String): Long =
        try {
            TsidBase62Encoder.decode(clubId)
        } catch (e: IllegalArgumentException) {
            throw UserProfileInvalidClubIdException()
        }

    private fun validateDuplicateClubAssignments(assignments: List<DecodedClubProfileAssignment>) {
        if (assignments.map { it.clubId }.distinct().size != assignments.size) {
            throw UserProfileDuplicateClubAssignmentException()
        }
    }

    private fun assignCreatedProfileIfRequested(
        userId: Long,
        profile: UserProfile,
        clubIds: List<String>,
    ) {
        if (clubIds.isEmpty()) return

        val assignments =
            clubIds.map {
                DecodedClubProfileAssignment(
                    clubId = decodeClubId(it),
                    profileId = profile.id,
                )
            }
        validateDuplicateClubAssignments(assignments)

        val membersByClubId = findAssignableMembers(userId, assignments.map { it.clubId }.distinct().sorted())
        assignments.forEach { assignment ->
            val member = membersByClubId[assignment.clubId] ?: throw UserProfileAssignmentNotAllowedException()
            member.assignProfile(profile)
        }
    }

    private fun findOwnedProfiles(
        userId: Long,
        profileIds: List<Long>,
    ): Map<Long, UserProfile> {
        val profilesById =
            userProfileRepository
                .findAllByUserIdAndIdInWithLock(
                    userId,
                    profileIds,
                ).associateBy { it.id }
        if (profilesById.size != profileIds.size) {
            throw UserProfileNotFoundException()
        }
        return profilesById
    }

    private fun findAssignableMembers(
        userId: Long,
        clubIds: List<Long>,
    ): Map<Long, ClubMember> {
        val membersByClubId =
            clubMemberRepository
                .findAllActiveByUserIdAndClubIdsWithLock(userId, clubIds)
                .associateBy { it.club.id }
        if (membersByClubId.size != clubIds.size) {
            throw UserProfileAssignmentNotAllowedException()
        }
        return membersByClubId
    }

    private data class DecodedClubProfileAssignment(
        val clubId: Long,
        val profileId: Long,
    )
}
