package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.converter.EmailConverter
import com.weeth.domain.user.domain.converter.PhoneNumberConverter
import com.weeth.domain.user.domain.enums.Role
import com.weeth.domain.user.domain.enums.Status
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
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User protected constructor() : BaseEntity() { // todo: 엔티티 정리 (생성자 정리, lateinit 제거 등)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, length = 50)
    lateinit var name: String
        private set

    @Convert(converter = EmailConverter::class)
    @Column(name = "email", nullable = false, length = 255)
    lateinit var email: Email
        private set

    @Column(nullable = false, length = 20)
    lateinit var studentId: String
        private set

    @Convert(converter = PhoneNumberConverter::class)
    @Column(name = "tel", nullable = false, length = 20)
    lateinit var tel: PhoneNumber
        private set

    @Column(nullable = false, length = 100)
    lateinit var department: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: Status = Status.WAITING
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER
        private set

    @Embedded
    var attendanceStats: AttendanceStats = AttendanceStats()
        private set

    @Column(nullable = false)
    var penaltyCount: Int = 0
        private set

    constructor(
        id: Long = 0L,
        name: String,
        email: Email,
        studentId: String = "",
        tel: PhoneNumber = PhoneNumber.from(""),
        department: String = "",
        status: Status = Status.WAITING,
        role: Role = Role.USER,
        attendanceStats: AttendanceStats = AttendanceStats(),
        penaltyCount: Int = 0,
    ) : this() {
        this.id = id
        this.name = name.trim()
        this.email = email
        this.studentId = studentId
        this.tel = tel
        this.department = department
        this.status = status
        this.role = role
        this.attendanceStats = attendanceStats
        this.penaltyCount = penaltyCount
    }

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

    fun leave() {
        status = Status.LEFT
    }

    fun isActive(): Boolean = status == Status.ACTIVE

    fun isInactive(): Boolean = !isActive()

    fun isBannedOrLeft(): Boolean = status == Status.BANNED || status == Status.LEFT

    fun isProfileCompleted(): Boolean =
        name.isNotBlank() &&
            studentId.isNotBlank() &&
            telValue.isNotBlank() &&
            department.isNotBlank()

    fun update(
        name: String,
        email: Email,
        studentId: String,
        tel: PhoneNumber,
        department: String,
    ) {
        require(name.isNotBlank()) { "이름은 공백일 수 없습니다." }
        this.name = name.trim()
        this.email = email
        this.studentId = studentId
        this.tel = tel
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

    fun hasRole(role: Role): Boolean = this.role == role

    companion object {
        fun create(
            name: String,
            email: String,
            studentId: String = "",
            tel: String = "",
            department: String = "",
            status: Status = Status.WAITING,
        ): User =
            User(
                name = name,
                email = Email.from(email),
                studentId = studentId,
                tel = PhoneNumber.from(tel),
                department = department,
                status = status,
            )
    }
}
