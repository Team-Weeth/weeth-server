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

    fun accept() {
        check(memberStatus == MemberStatus.WAITING) { "대기 상태인 멤버만 승인할 수 있습니다." }
        memberStatus = MemberStatus.ACTIVE
    }

    fun ban() {
        check(memberStatus != MemberStatus.BANNED) { "이미 차단된 멤버입니다." }
        check(memberStatus != MemberStatus.LEFT) { "탈퇴한 멤버는 차단할 수 없습니다." }
        memberStatus = MemberStatus.BANNED
    }

    fun leave() {
        check(memberStatus == MemberStatus.ACTIVE) { "활동 중인 멤버만 탈퇴할 수 있습니다." }
        memberStatus = MemberStatus.LEFT
    }

    fun isActive(): Boolean = memberStatus == MemberStatus.ACTIVE

    fun updateRole(role: MemberRole) {
        this.memberRole = role
    }

    fun isAdmin(): Boolean = memberRole == MemberRole.ADMIN

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

    fun incrementPenaltyCount() {
        penaltyCount++
    }

    fun decrementPenaltyCount() {
        if (penaltyCount > 0) {
            penaltyCount--
        }
    }

    companion object {
        fun create(
            club: Club,
            user: User,
            memberRole: MemberRole = MemberRole.USER,
        ): ClubMember {
            return ClubMember(club = club, user = user, memberRole = memberRole)
        }
    }
}
