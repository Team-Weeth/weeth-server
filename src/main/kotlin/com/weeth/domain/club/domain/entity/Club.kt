package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.global.common.entity.BaseEntity
import com.weeth.global.common.id.TsidGenerator
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "club",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_club_school_name_club_name",
            columnNames = ["school_name", "name"],
        ),
    ],
)
class Club(
    name: String,
    code: String,
    description: String? = null,
    schoolName: String,
    clubContact: ClubContact,
    profileImageStorageKey: String? = null,
    backgroundImageStorageKey: String? = null,
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

    @Column(nullable = false, unique = true, length = 36)
    var code: String = code
        private set

    @Column(length = 30)
    var description: String? = description
        private set

    @Column(length = 50)
    var schoolName: String = schoolName
        private set

    @Embedded
    var clubContact: ClubContact = clubContact
        private set

    @Column(name = "profile_image_url", length = 500)
    var profileImageStorageKey: String? = profileImageStorageKey
        private set

    @Column(name = "background_image_url", length = 500)
    var backgroundImageStorageKey: String? = backgroundImageStorageKey
        private set

    @Column(nullable = false)
    var warningEnabled: Boolean = false
        private set

    @Column(length = 500, nullable = true)
    var penaltyRule: String? = null
        private set

    /**
     * 이 동아리가 보유할 수 있는 활성 게시판 수.
     *
     * 요금제별 차등을 위해 club 단위로 관리한다. 상수로 두면 전 동아리가 같은 값을 쓰게 되고,
     * 운영상 예외(예: 마이그레이션으로 게시판이 늘어난 동아리)를 개별 조정할 수 없다.
     */
    @Column(name = "max_board_count", nullable = false)
    var maxBoardCount: Int = DEFAULT_MAX_BOARD_COUNT
        private set

    // todo: 동아리 삭제 지원

    fun update(
        name: String?,
        schoolName: String?,
        description: String?,
        contactEmail: String?,
        contactPhoneNumber: String?,
        primaryContact: PrimaryContact?,
        profileImageStorageKey: String?,
        backgroundImageStorageKey: String?,
    ) {
        name?.let {
            require(it.isNotBlank()) { "동아리 이름은 비어 있을 수 없습니다." }
            this.name = it.trim()
        }
        schoolName?.let {
            require(it.isNotBlank()) { "학교 이름은 비어 있을 수 없습니다." }
            this.schoolName = it.trim()
        }
        description?.let {
            require(it.length <= MAX_DESCRIPTION_LENGTH) { "소개글은 ${MAX_DESCRIPTION_LENGTH}자 이하여야 합니다." }
            this.description = it
        }

        updateContact(contactEmail, contactPhoneNumber, primaryContact)
        updateImageStorageKey(profileImageStorageKey, backgroundImageStorageKey)
    }

    private fun updateContact(
        contactEmail: String?,
        contactPhoneNumber: String?,
        primaryContact: PrimaryContact?,
    ) {
        if (contactEmail != null || contactPhoneNumber != null || primaryContact != null) {
            clubContact.update(
                email = contactEmail,
                phoneNumber = contactPhoneNumber,
                primaryContact = primaryContact,
            )
        }
    }

    private fun updateImageStorageKey(
        profileImageStorageKey: String?,
        backgroundImageStorageKey: String?,
    ) {
        if (profileImageStorageKey != null || backgroundImageStorageKey != null) {
            this.profileImageStorageKey = profileImageStorageKey ?: this.profileImageStorageKey
            this.backgroundImageStorageKey = backgroundImageStorageKey ?: this.backgroundImageStorageKey
        }
    }

    fun updatePenaltyRule(rule: String?) {
        rule?.let {
            require(it.length <= MAX_PENALTY_RULE_LENGTH) { "패널티 규정은 ${MAX_PENALTY_RULE_LENGTH}자 이하여야 합니다." }
        }
        this.penaltyRule = rule?.takeIf { it.isNotBlank() }
    }

    fun regenerateCode(newCode: String) {
        require(newCode.isNotBlank()) { "초대 코드는 비어 있을 수 없습니다." }
        this.code = newCode
    }

    fun removeProfileImage() {
        this.profileImageStorageKey = null
    }

    fun removeBackgroundImage() {
        this.backgroundImageStorageKey = null
    }

    /**
     * 활성 게시판을 하나 더 만들 수 있는지 판단한다.
     *
     * @param currentActiveCount 삭제되지 않은 게시판 수
     */
    fun canAddBoard(currentActiveCount: Int): Boolean = currentActiveCount < maxBoardCount

    /**
     * 게시판 상한을 조정한다.
     *
     * 현재 보유 수보다 낮게 내리는 것 자체는 막지 않는다. 기존 게시판을 강제로 지울 수는 없고,
     * 상한 검사는 생성 시점에만 이루어지므로 "더 만들 수 없다"는 의미로 동작하면 충분하다.
     */
    fun changeMaxBoardCount(count: Int) {
        require(count >= MIN_BOARD_COUNT) { "게시판 상한은 ${MIN_BOARD_COUNT}개 이상이어야 합니다." }
        require(count <= MAX_BOARD_COUNT_LIMIT) { "게시판 상한은 ${MAX_BOARD_COUNT_LIMIT}개 이하여야 합니다." }
        this.maxBoardCount = count
    }

    @PrePersist
    fun assignIdIfAbsent() {
        if (id == 0L) {
            id = TsidGenerator.nextId()
        }
    }

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 30
        private const val MAX_PENALTY_RULE_LENGTH = 500

        /** 신규 동아리 기본 상한. 요금제 도입 전까지는 모든 동아리가 이 값으로 생성된다. */
        const val DEFAULT_MAX_BOARD_COUNT = 4

        /** 공지사항은 동아리 생성 시 자동 제공되므로 최소 1개는 보장한다. */
        private const val MIN_BOARD_COUNT = 1

        /** 무한정 늘어나 목록 UI가 무너지는 것을 막는 안전장치. */
        private const val MAX_BOARD_COUNT_LIMIT = 30

        fun create(
            name: String,
            code: String,
            schoolName: String,
            clubContact: ClubContact,
            description: String? = null,
            profileImageStorageKey: String? = null,
            backgroundImageStorageKey: String? = null,
        ): Club {
            require(name.isNotBlank()) { "동아리 이름은 비어 있을 수 없습니다." }
            require(code.isNotBlank()) { "초대 코드는 비어 있을 수 없습니다." }
            require(schoolName.isNotBlank()) { "학교 이름은 비어 있을 수 없습니다." }
            description?.let {
                require(it.length <= MAX_DESCRIPTION_LENGTH) { "소개글은 ${MAX_DESCRIPTION_LENGTH}자 이하여야 합니다." }
            }
            return Club(
                name = name,
                code = code,
                description = description,
                schoolName = schoolName,
                clubContact = clubContact,
                profileImageStorageKey = profileImageStorageKey,
                backgroundImageStorageKey = backgroundImageStorageKey,
            ).apply {
                // 객체 생성시 TSID 할당
                id = TsidGenerator.nextId()
            }
        }
    }
}
