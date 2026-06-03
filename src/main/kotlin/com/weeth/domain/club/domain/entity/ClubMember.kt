package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.vo.ClubAttendanceStats
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "club_member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_club_id_user_id",
            columnNames = ["club_id", "user_id"],
        ),
    ],
)
class ClubMember(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: Club,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    memberStatus: MemberStatus = MemberStatus.WAITING,
    memberRole: MemberRole = MemberRole.USER,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_id")
    var id: Long = 0L
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var memberStatus: MemberStatus = memberStatus
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var memberRole: MemberRole = memberRole
        private set

    @Embedded
    var attendanceStats: ClubAttendanceStats = ClubAttendanceStats()
        private set

    @Column(nullable = false)
    var penaltyCount: Int = 0
        private set

    @Column(length = 500)
    var profileImageStorageKey: String? = null
        private set

    @Column(length = 30)
    var bio: String? = null
        private set

    @Column(name = "left_at")
    var leftAt: LocalDateTime? = null
        private set

    @Column(name = "hard_delete_after")
    var hardDeleteAfter: LocalDateTime? = null
        private set

    fun accept() {
        check(memberStatus == MemberStatus.WAITING) { "대기 상태인 멤버만 승인할 수 있습니다." }
        memberStatus = MemberStatus.ACTIVE
        // TODO: BANNED 복구가 필요해지면 accept()에 섞지 말고 별도 unban/restore 정책과 API로 분리
    }

    fun ban() {
        check(memberStatus != MemberStatus.BANNED) { "이미 차단된 멤버입니다." }
        check(memberStatus != MemberStatus.LEFT) { "탈퇴한 멤버는 차단할 수 없습니다." }
        memberStatus = MemberStatus.BANNED
    }

    fun restore() {
        check(memberStatus == MemberStatus.BANNED) { "차단된 멤버만 복구할 수 있습니다." }
        memberStatus = MemberStatus.ACTIVE
    }

    fun leave(now: LocalDateTime) {
        check(memberStatus == MemberStatus.ACTIVE) { "활동 중인 멤버만 탈퇴할 수 있습니다." }
        memberStatus = MemberStatus.LEFT
        leftAt = now
        hardDeleteAfter = now.plusDays(RETENTION_DAYS)
    }

    fun isActive(): Boolean = memberStatus == MemberStatus.ACTIVE

    fun updateRole(role: MemberRole) {
        check(role != MemberRole.LEAD) { "LEAD는 이양을 통해서만 변경할 수 있습니다." }
        check(!isLead()) { "LEAD의 권한은 이양을 통해서만 변경할 수 있습니다." }
        this.memberRole = role
    }

    fun isAdminOrLead(): Boolean = memberRole.isAdminOrLead()

    fun isLead(): Boolean = memberRole == MemberRole.LEAD

    fun releaseLead() {
        check(isLead()) { "LEAD만 권한을 내려놓을 수 있습니다." }
        this.memberRole = MemberRole.ADMIN
    }

    fun assignLead() {
        this.memberRole = MemberRole.LEAD
    }

    fun attend() {
        attendanceStats.attend()
    }

    fun removeAttend() {
        attendanceStats.removeAttend()
    }

    fun absent() {
        attendanceStats.absent()
    }

    fun removeAbsent() {
        attendanceStats.removeAbsent()
    }

    fun resetAttendanceStats() {
        attendanceStats.reset()
    }

    fun recalculateAttendanceStats(
        attendCount: Int,
        absentCount: Int,
    ) {
        attendanceStats.recalculate(attendCount, absentCount)
    }

    fun incrementPenaltyCount() {
        penaltyCount++
    }

    fun resetPenaltyCount() {
        penaltyCount = 0
    }

    fun recalculatePenaltyCount(count: Int) {
        require(count >= 0) { "패널티 수는 0 이상이어야 합니다." }
        penaltyCount = count
    }

    fun updateProfileImageUrl(storageKey: String?) {
        val trimmed = storageKey?.trim()?.takeIf { it.isNotBlank() }
        require((trimmed?.length ?: 0) <= 500) { "프로필 이미지 storageKey는 500자 이하여야 합니다." }
        this.profileImageStorageKey = trimmed
    }

    fun removeProfileImage() {
        this.profileImageStorageKey = null
    }

    fun updateBio(bio: String?) {
        val trimmed = bio?.trim()?.takeIf { it.isNotBlank() }
        require((trimmed?.length ?: 0) <= 30) { "자기소개는 30자 이하여야 합니다." }
        this.bio = trimmed
    }

    fun decrementPenaltyCount() {
        if (penaltyCount > 0) {
            penaltyCount--
        }
    }

    companion object {
        private const val RETENTION_DAYS = 30L

        fun create(
            club: Club,
            user: User,
            memberRole: MemberRole = MemberRole.USER,
        ): ClubMember = ClubMember(club = club, user = user, memberRole = memberRole)
    }
}
