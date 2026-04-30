package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.session.domain.entity.Session
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
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
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(name = "uk_attendance_session_member", columnNames = ["session_id", "club_member_id"]),
    ],
)
class Attendance(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    val session: Session,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    val clubMember: ClubMember,
    status: AttendanceStatus = AttendanceStatus.PENDING,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    var id: Long = 0L
        private set

    @Enumerated(EnumType.STRING)
    var status: AttendanceStatus = status
        private set

    fun attend() {
        check(status == AttendanceStatus.PENDING) { "이미 처리된 출석입니다" }
        status = AttendanceStatus.ATTEND
    }

    fun absent() {
        check(status == AttendanceStatus.PENDING) { "이미 처리된 출석입니다" }
        status = AttendanceStatus.ABSENT
    }

    // 기존 close() 는 absent() 로 대체 (AttendanceUpdateService 호환 유지)
    fun close() = absent()

    fun adminOverride(newStatus: AttendanceStatus) {
        status = newStatus
    }

    fun isPending(): Boolean = status == AttendanceStatus.PENDING

    fun isWrong(code: Int): Boolean = !session.isCodeMatch(code)

    companion object {
        fun create(
            session: Session,
            clubMember: ClubMember,
        ): Attendance {
            require(session.club.id == clubMember.club.id) { "세션과 멤버의 동아리가 일치하지 않습니다" }
            return Attendance(session = session, clubMember = clubMember)
        }
    }
}
