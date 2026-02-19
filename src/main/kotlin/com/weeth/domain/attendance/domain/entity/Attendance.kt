package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.user.domain.entity.User
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

@Entity
class Attendance(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    val session: Session,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    @Enumerated(EnumType.STRING)
    var status: AttendanceStatus = AttendanceStatus.PENDING,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    val id: Long = 0

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

    fun isPending(): Boolean = status == AttendanceStatus.PENDING

    fun isWrong(code: Int): Boolean = !session.isCodeMatch(code)

    companion object {
        fun create(
            session: Session,
            user: User,
        ): Attendance = Attendance(session = session, user = user)
    }
}
