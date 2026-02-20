package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.converter.EmailConverter
import com.weeth.domain.user.domain.converter.PhoneNumberConverter
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.vo.AttendanceStats
import com.weeth.domain.user.domain.vo.Email
import com.weeth.domain.user.domain.vo.PhoneNumber
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long = 0L,
    var name: String = "",
    @Convert(converter = EmailConverter::class)
    @Column(name = "email")
    var email: Email = Email.from(""),
    var studentId: String = "",
    @Convert(converter = PhoneNumberConverter::class)
    @Column(name = "tel")
    var tel: PhoneNumber = PhoneNumber.from(""),
    var department: String = "",
    @Enumerated(EnumType.STRING)
    var status: Status = Status.WAITING,
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER,
    @Embedded
    var attendanceStats: AttendanceStats = AttendanceStats(),
    var penaltyCount: Int = 0,
    var warningCount: Int = 0, // todo: 경고시 자동 페널티 기능도 제거
) : BaseEntity() {
    constructor(
        id: Long = 0L,
        name: String = "",
        email: String = "",
        studentId: String = "",
        tel: String = "",
        department: String = "",
        status: Status = Status.WAITING,
        role: Role = Role.USER,
        attendanceCount: Int = 0,
        absenceCount: Int = 0,
        attendanceRate: Int = 0,
        penaltyCount: Int = 0,
        warningCount: Int = 0,
    ) : this(
        id = id,
        name = name,
        email = Email.from(email),
        studentId = studentId,
        tel = PhoneNumber.from(tel),
        department = department,
        status = status,
        role = role,
        attendanceStats = AttendanceStats(attendanceCount, absenceCount, attendanceRate),
        penaltyCount = penaltyCount,
        warningCount = warningCount,
    )

    val emailValue: String
        get() = email.value

    val telValue: String
        get() = tel.value

    val attendanceCount: Int
        get() = attendanceStats.attendanceCount

    val absenceCount: Int
        get() = attendanceStats.absenceCount

    val attendanceRate: Int
        get() = attendanceStats.attendanceRate

    @PrePersist
    fun init() {
        status = Status.WAITING
        role = Role.USER
        attendanceStats.reset()
        penaltyCount = 0
        warningCount = 0
    }

    fun leave() {
        status = Status.LEFT
    }

    fun isInactive(): Boolean = status != Status.ACTIVE

    fun isProfileCompleted(): Boolean =
        name.isNotBlank() &&
            studentId.isNotBlank() &&
            telValue.isNotBlank() &&
            department.isNotBlank()

    fun update(
        name: String,
        email: String,
        studentId: String,
        tel: String,
        department: String,
    ) {
        this.name = name
        this.email = Email.from(email)
        this.studentId = studentId
        this.tel = PhoneNumber.from(tel)
        this.department = department
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

    fun resetAttendanceStats() {
        attendanceStats.reset()
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

    fun incrementPenaltyCount() {
        penaltyCount++
    }

    fun decrementPenaltyCount() {
        if (penaltyCount > 0) {
            penaltyCount--
        }
    }

    fun incrementWarningCount() {
        warningCount++
    }

    fun decrementWarningCount() {
        if (warningCount > 0) {
            warningCount--
        }
    }

    fun hasRole(role: Role): Boolean = this.role == role

    companion object {
        fun create(
            name: String,
            email: String,
            studentId: String,
            tel: String,
            department: String,
        ): User =
            User(
                name = name,
                email = Email.from(email),
                studentId = studentId,
                tel = PhoneNumber.from(tel),
                department = department,
            )
    }
}
