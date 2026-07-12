package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageUserProfileUseCase(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
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

        return userProfileMapper.toResponse(savedProfile)
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
}
