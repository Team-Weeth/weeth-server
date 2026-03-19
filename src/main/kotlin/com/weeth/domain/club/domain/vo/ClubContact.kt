package com.weeth.domain.club.domain.vo

import com.weeth.domain.club.domain.enums.PrimaryContact
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * 동아리 연락처를 저장하기 위한 VO.
 * 전화번호는 필수이며, 이메일은 선택 사항이다.
 * primaryContact는 주 연락처를 나타낸다. EMAIL을 선택하려면 이메일이 반드시 존재해야 한다.
 */
@Embeddable
class ClubContact(
    email: String? = null,
    phoneNumber: String,
    primaryContact: PrimaryContact,
) {
    @Column(name = "contact_email", length = 100)
    var email: String? = email
        private set

    @Column(name = "contact_phone_number", nullable = false, length = 20)
    var phoneNumber: String = phoneNumber
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_contact", nullable = false, length = 10)
    var primaryContact: PrimaryContact = primaryContact
        private set

    fun update(
        email: String?,
        phoneNumber: String?,
        primaryContact: PrimaryContact?,
    ) {
        phoneNumber?.let {
            require(it.isNotBlank()) { "전화번호는 비어 있을 수 없습니다." }
            this.phoneNumber = it
        }
        this.email = email ?: this.email
        primaryContact?.let {
            if (it == PrimaryContact.EMAIL) {
                val resolvedEmail = email ?: this.email
                require(resolvedEmail != null) { "주 연락처를 이메일로 설정하려면 이메일을 입력해야 합니다." }
            }
            this.primaryContact = it
        }
    }

    companion object {
        fun from(
            email: String?,
            phoneNumber: String,
            primaryContact: PrimaryContact,
        ): ClubContact {
            if (primaryContact == PrimaryContact.EMAIL) {
                require(email != null) { "주 연락처를 이메일로 설정하려면 이메일을 입력해야 합니다." }
            }
            return ClubContact(email = email, phoneNumber = phoneNumber, primaryContact = primaryContact)
        }
    }
}
