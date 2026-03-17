package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.converter.EmailConverter
import com.weeth.domain.user.domain.converter.PhoneNumberConverter
import com.weeth.domain.user.domain.enums.Role
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.vo.Email
import com.weeth.domain.user.domain.vo.PhoneNumber
import com.weeth.global.common.entity.BaseEntity
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
class User protected constructor() : BaseEntity() { // todo: 엔티티 정리 (생성자 정리, lateinit 제거 등)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, length = 50)
    lateinit var name: String
        private set

    @Convert(converter = EmailConverter::class)
    @Column(name = "email", nullable = false, length = 255)
    lateinit var email: Email
        private set

    @Column(nullable = false, length = 20)
    lateinit var studentId: String
        private set

    @Convert(converter = PhoneNumberConverter::class)
    @Column(name = "tel", nullable = false, length = 20)
    lateinit var tel: PhoneNumber
        private set

    @Column(nullable = false, length = 100)
    lateinit var department: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: Status = Status.WAITING
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER
        private set

    @Column(nullable = false)
    var termsAgreed: Boolean = false
        private set

    @Column(nullable = false)
    var privacyAgreed: Boolean = false
        private set

    @Column(length = 200)
    var bio: String? = null
        private set

    @Column(length = 500)
    var profileImageUrl: String? = null
        private set

    constructor(
        id: Long = 0L,
        name: String,
        email: Email,
        studentId: String = "",
        tel: PhoneNumber = PhoneNumber.from(""),
        department: String = "",
        status: Status = Status.WAITING,
        role: Role = Role.USER,
        profileImageUrl: String? = null,
    ) : this() {
        this.id = id
        this.name = name.trim()
        this.email = email
        this.studentId = studentId
        this.tel = tel
        this.department = department
        this.status = status
        this.role = role
        this.profileImageUrl = profileImageUrl
    }

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
            department.isNotBlank()

    fun update(
        name: String,
        email: Email,
        studentId: String,
        tel: PhoneNumber,
        department: String,
        bio: String?,
    ) {
        require(name.isNotBlank()) { "이름은 공백일 수 없습니다." }
        this.name = name.trim()
        this.email = email
        this.studentId = studentId
        this.tel = tel
        this.department = department
        this.bio = bio?.trim()?.takeIf { it.isNotBlank() }
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
        this.profileImageUrl = url?.trim()
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
            department: String = "",
            status: Status = Status.WAITING,
            profileImageUrl: String? = null,
        ): User =
            User(
                name = name,
                email = Email.from(email),
                studentId = studentId,
                tel = PhoneNumber.from(tel),
                department = department,
                status = status,
                profileImageUrl = profileImageUrl,
            )
    }
}
