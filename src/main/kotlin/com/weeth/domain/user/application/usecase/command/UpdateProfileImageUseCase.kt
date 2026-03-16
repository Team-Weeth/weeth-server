package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateProfileImageUseCase(
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    @Transactional
    fun execute(
        userId: Long,
        request: FileSaveRequest,
    ) {
        val user = userRepository.getById(userId)

        fileRepository
            .findAllByOwnerTypeAndOwnerIdAndStatus(
                FileOwnerType.USER_PROFILE,
                user.id,
                FileStatus.UPLOADED,
            ).forEach { it.markDeleted() }

        val file =
            File.createUploaded(
                fileName = request.fileName,
                storageKey = request.storageKey,
                fileSize = request.fileSize,
                contentType = request.contentType,
                ownerType = FileOwnerType.USER_PROFILE,
                ownerId = user.id,
            )
        fileRepository.save(file)

        user.updateProfileImageUrl(fileAccessUrlPort.resolve(file.storageKey.value))
    }
}
