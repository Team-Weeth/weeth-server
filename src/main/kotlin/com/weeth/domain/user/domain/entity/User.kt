package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.converter.EmailConverter
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import com.weeth.global.common.converter.PhoneNumberConverter
import com.weeth.global.common.entity.BaseEntity
import com.weeth.global.common.vo.PhoneNumber
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    name: String,
    email: Email,
    studentId: String? = null,
    tel: PhoneNumber? = null,
    school: String? = null,
    department: String? = null,
    status: Status = Status.WAITING,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, length = 50)
    var name: String = name.trim().also { require(it.isNotBlank()) { "이름은 공백일 수 없습니다." } }
        private set

    @Convert(converter = EmailConverter::class)
    @Column(name = "email", nullable = false, length = 255)
    var email: Email = email
        private set

    @Column(nullable = true, length = 20)
    var studentId: String? = studentId
        private set

    @Convert(converter = PhoneNumberConverter::class)
    @Column(name = "tel", nullable = true, length = 20)
    var tel: PhoneNumber? = tel
        private set

    @Column(nullable = true, length = 50)
    var school: String? = school
        private set

    @Column(nullable = true, length = 100)
    var department: String? = department
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: Status = status
        private set

    @Column(nullable = false)
    var termsAgreed: Boolean = false
        private set

    @Column(nullable = false)
    var privacyAgreed: Boolean = false
        private set

    val emailValue: String
        get() = email.value

    val telValue: String?
        get() = tel?.value

    fun leave() {
        status = Status.LEFT
    }

    fun isActive(): Boolean = status == Status.ACTIVE

    fun isInactive(): Boolean = !isActive()

    fun isBannedOrLeft(): Boolean = status == Status.BANNED || status == Status.LEFT

    fun isRegistered(): Boolean = status == Status.ACTIVE && termsAgreed && privacyAgreed

    fun isProfileCompleted(): Boolean = missingProfileFields().isEmpty()

    fun missingProfileFields(): List<String> =
        buildList {
            if (studentId.isNullOrBlank()) add("studentId")
            if (telValue.isNullOrBlank()) add("tel")
            if (school.isNullOrBlank()) add("school")
            if (department.isNullOrBlank()) add("department")
        }

    fun update(
        name: String? = null,
        email: Email? = null,
        studentId: String? = null,
        tel: PhoneNumber? = null,
        school: String? = null,
        department: String? = null,
    ) {
        name?.let {
            require(it.isNotBlank()) { "이름은 공백일 수 없습니다." }
            this.name = it.trim()
        }
        email?.let { this.email = it }
        studentId?.let { this.studentId = it }
        tel?.let { this.tel = it }
        school?.let { this.school = it }
        department?.let { this.department = it }
    }

    fun agreeTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
    ) {
        require(termsAgreed && privacyAgreed) { "모든 약관에 동의해야 합니다." }
        this.termsAgreed = true
        this.privacyAgreed = true
    }

    fun accept() {
        status = Status.ACTIVE
    }

    fun ban() {
        status = Status.BANNED
    }

    companion object {
        fun create(
            name: String,
            email: String,
            studentId: String? = null,
            tel: String? = null,
            school: String? = null,
            department: String? = null,
            status: Status = Status.WAITING,
        ): User =
            User(
                name = name,
                email = Email.from(email),
                studentId = studentId,
                tel = tel?.let { PhoneNumber.from(it) },
                school = school,
                department = department,
                status = status,
            )
    }
}
