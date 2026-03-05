package com.weeth.domain.club.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 동아리 연락처를 저장하기 위한 VO.
 * email 혹은 phoneNumber 둘 중 하나는 반드시 존재해야 하며, 값이 있다면 둘 다 저장 가능.
 */
@Embeddable
class ClubContact(
    email: String? = null,
    phoneNumber: String? = null,
) {
    @Column(name = "contact_email", length = 100)
    var email: String? = email
        private set

    @Column(name = "contact_phone_number", length = 20)
    var phoneNumber: String? = phoneNumber
        private set

    fun update(
        email: String?,
        phoneNumber: String?,
    ) {
        require(email != null || phoneNumber != null) { "이메일 또는 전화번호 중 하나는 반드시 입력해야 합니다." }
        this.email = email
        this.phoneNumber = phoneNumber
    }

    companion object {
        fun from(
            email: String?,
            phoneNumber: String?,
        ): ClubContact {
            require(email != null || phoneNumber != null) { "이메일 또는 전화번호 중 하나는 반드시 입력해야 합니다." }
            return ClubContact(email = email, phoneNumber = phoneNumber)
        }
    }
}
