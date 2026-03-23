package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.converter.EmailConverter
import com.weeth.domain.user.domain.enums.Role
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
    studentId: String = "",
    tel: PhoneNumber = PhoneNumber.from(""),
    school: String = "",
    department: String = "",
    status: Status = Status.WAITING,
    role: Role = Role.USER,
    profileImageUrl: String? = null,
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

    @Column(nullable = false, length = 20)
    var studentId: String = studentId
        private set

    @Convert(converter = PhoneNumberConverter::class)
    @Column(name = "tel", nullable = false, length = 20)
    var tel: PhoneNumber = tel
        private set

    @Column(nullable = false, length = 50)
    var school: String = school
        private set

    @Column(nullable = false, length = 100)
    var department: String = department
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: Status = status
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = role
        private set

    @Column(nullable = false)
    var termsAgreed: Boolean = false
        private set

    @Column(nullable = false)
    var privacyAgreed: Boolean = false
        private set

    @Column(length = 500)
    var profileImageUrl: String? = profileImageUrl?.trim()?.takeIf { it.isNotBlank() }
        private set

    val emailValue: String
        get() = email.value

    val telValue: String
        get() = tel.value

    fun leave() {
        status = Status.LEFT
    }

    fun isActive(): Boolean = status == Status.ACTIVE

    fun isInactive(): Boolean = !isActive()

    fun isBannedOrLeft(): Boolean = status == Status.BANNED || status == Status.LEFT

    fun isProfileCompleted(): Boolean =
        name.isNotBlank() &&
            studentId.isNotBlank() &&
            telValue.isNotBlank() &&
            school.isNotBlank() &&
            department.isNotBlank()

    fun update(
        name: String,
        email: Email,
        studentId: String,
        tel: PhoneNumber,
        school: String,
        department: String,
    ) {
        require(name.isNotBlank()) { "이름은 공백일 수 없습니다." }
        this.name = name.trim()
        this.email = email
        this.studentId = studentId
        this.tel = tel
        this.school = school
        this.department = department
    }

    fun agreeTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
    ) {
        require(termsAgreed && privacyAgreed) { "모든 약관에 동의해야 합니다." }
        this.termsAgreed = termsAgreed
        this.privacyAgreed = privacyAgreed
    }

    fun updateProfileImageUrl(url: String?) {
        this.profileImageUrl = url?.trim()?.takeIf { it.isNotBlank() }
    }

    fun accept() {
        status = Status.ACTIVE
    }

    fun ban() {
        status = Status.BANNED
    }

    fun updateRole(role: Role) {
        this.role = role
    }

    fun hasRole(role: Role): Boolean = this.role == role

    companion object {
        fun create(
            name: String,
            email: String,
            studentId: String = "",
            tel: String = "",
            school: String = "",
            department: String = "",
            status: Status = Status.WAITING,
            profileImageUrl: String? = null,
        ): User =
            User(
                name = name,
                email = Email.from(email),
                studentId = studentId,
                tel = PhoneNumber.from(tel),
                school = school,
                department = department,
                status = status,
                profileImageUrl = profileImageUrl,
            )
    }
}
