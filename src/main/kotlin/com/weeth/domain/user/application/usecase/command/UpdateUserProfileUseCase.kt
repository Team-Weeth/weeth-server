package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.exception.StudentIdExistsException
import com.weeth.domain.user.application.exception.TelExistsException
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.vo.Email
import com.weeth.global.common.vo.PhoneNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserProfileUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateProfile(
        request: UpdateUserProfileRequest,
        userId: Long,
    ) {
        validate(request, userId)
        val user = userRepository.getById(userId)
        user.update(
            name = request.name,
            email = Email.from(request.email),
            studentId = request.studentId,
            tel = PhoneNumber.from(request.tel),
            school = request.school,
            department = request.department,
        )
    }

    private fun validate(
        request: UpdateUserProfileRequest,
        userId: Long,
    ) {
        if (userRepository.existsByStudentIdAndIdIsNot(request.studentId, userId)) {
            throw StudentIdExistsException()
        }
        if (userRepository.existsByTelAndIdIsNotValue(request.tel, userId)) {
            throw TelExistsException()
        }
    }
}
