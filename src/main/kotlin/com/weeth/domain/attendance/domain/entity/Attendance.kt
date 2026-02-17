package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist

@Entity
class Attendance
    @JvmOverloads
    constructor(
        @ManyToOne
        @JoinColumn(name = "meeting_id")
        val meeting: Meeting,
        @ManyToOne
        @JoinColumn(name = "user_id")
        val user: User,
        @Enumerated(EnumType.STRING)
        var status: Status? = null,
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "attendance_id")
        val id: Long = 0,
    ) : BaseEntity() {
        @PrePersist
        fun init() {
            status = Status.PENDING
        }

        fun attend() {
            status = Status.ATTEND
        }

        fun close() {
            status = Status.ABSENT
        }

        val isPending: Boolean
            get() = status == Status.PENDING

        fun isWrong(code: Int): Boolean = meeting.getCode() != code
    }
