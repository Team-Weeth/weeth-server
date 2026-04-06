package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.exception.ProfileRequiredFieldsMissingException
import com.weeth.domain.user.application.exception.StudentIdExistsException
import com.weeth.domain.user.application.exception.TelExistsException
import com.weeth.domain.user.domain.entity.User
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
        val user = userRepository.getById(userId)
        if (!user.isProfileCompleted()) {
            validateRequiredFields(request)
        }
        validateDuplicate(request, userId)
        user.update(
            name = request.name,
            email = request.email?.let { Email.from(it) },
            studentId = request.studentId,
            tel = request.tel?.let { PhoneNumber.from(it) },
            school = request.school,
            department = request.department,
        )
    }

    private fun validateRequiredFields(request: UpdateUserProfileRequest) {
        if (request.name == null ||
            request.email == null ||
            request.studentId == null ||
            request.tel == null ||
            request.school == null ||
            request.department == null
        ) {
            throw ProfileRequiredFieldsMissingException()
        }
    }

    private fun validateDuplicate(
        request: UpdateUserProfileRequest,
        userId: Long,
    ) {
        val school = request.school
        val studentId = request.studentId
        if (school != null && studentId != null &&
            userRepository.existsBySchoolAndStudentIdAndIdIsNot(school, studentId, userId)
        ) {
            throw StudentIdExistsException()
        }
        val tel = request.tel
        if (tel != null && userRepository.existsByTelAndIdIsNotValue(tel, userId)) {
            throw TelExistsException()
        }
    }
}
