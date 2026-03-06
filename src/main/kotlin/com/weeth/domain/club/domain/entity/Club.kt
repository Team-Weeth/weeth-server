package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.global.common.entity.BaseEntity
import com.weeth.global.common.id.TsidGenerator
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table

@Entity
@Table(name = "club")
class Club(
    name: String,
    code: String,
    description: String? = null,
    schoolName: String,
    clubContact: ClubContact,
) : BaseEntity() {
    // TSID(Time-Sorted Unique Identifier)로 관리
    // Client 반환시 Base62 인코딩해서 String으로 반환
    @Id
    @Column(name = "club_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, unique = false, length = 100)
    var name: String = name.trim()
        private set

    @Column(nullable = false, unique = true, length = 20)
    var code: String = code
        private set

    @Column(length = 100)
    var description: String? = description
        private set

    @Column(length = 50)
    var schoolName: String = schoolName
        private set

    @Embedded
    var clubContact: ClubContact = clubContact
        private set

    fun update(
        name: String,
        description: String?,
    ) {
        require(name.isNotBlank()) { "동아리 이름은 비어 있을 수 없습니다." }
        this.name = name.trim()
        this.description = description
    }

    fun updateContact(
        email: String?,
        phoneNumber: String?,
    ) {
        clubContact.update(email = email, phoneNumber = phoneNumber)
    }

    fun regenerateCode(newCode: String) {
        require(newCode.isNotBlank()) { "초대 코드는 비어 있을 수 없습니다." }
        this.code = newCode
    }

    @PrePersist
    fun assignIdIfAbsent() {
        if (id == 0L) {
            id = TsidGenerator.nextId()
        }
    }

    companion object {
        fun create(
            name: String,
            code: String,
            schoolName: String,
            clubContact: ClubContact,
            description: String? = null,
        ): Club {
            require(name.isNotBlank()) { "동아리 이름은 비어 있을 수 없습니다." }
            require(code.isNotBlank()) { "초대 코드는 비어 있을 수 없습니다." }
            require(schoolName.isNotBlank()) { "학교 이름은 비어 있을 수 없습니다." }
            return Club(
                name = name,
                code = code,
                description = description,
                schoolName = schoolName,
                clubContact = clubContact,
            ).apply {
                // 객체 생성시 TSID 할당
                id = TsidGenerator.nextId()
            }
        }
    }
}
