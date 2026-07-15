package com.weeth.domain.account.domain.entity

import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 회비 기능의 club 단위 노출 설정. club당 1행만 존재한다.
 * 기존 기수별(`Account.memberVisible`) 공개 여부를 대체해, 회비 기능 전체를 한 번에 공개/비공개한다.
 */
@Entity
@Table(
    name = "account_setting",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_setting_club",
            columnNames = ["club_id"],
        ),
    ],
)
class AccountSetting(
    clubId: Long,
    id: Long = 0,
    memberVisible: Boolean = false, // 회비 기능 부원 공개 여부 (기본 비공개)
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_setting_id")
    var id: Long = id
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Column(nullable = false)
    var memberVisible: Boolean = memberVisible
        private set

    fun showToMembers() {
        memberVisible = true
    }

    fun hideFromMembers() {
        memberVisible = false
    }

    companion object {
        fun createDefault(clubId: Long): AccountSetting {
            require(clubId > 0) { "동아리 ID는 0보다 커야 합니다: $clubId" }
            return AccountSetting(clubId = clubId)
        }
    }
}
