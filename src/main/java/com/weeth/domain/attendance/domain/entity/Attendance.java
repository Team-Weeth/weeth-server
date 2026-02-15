package com.weeth.domain.attendance.domain.entity;

import jakarta.persistence.*;
import com.weeth.domain.attendance.domain.entity.enums.Status;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void init() {
        this.status = Status.PENDING;
    }

    public Attendance(Meeting meeting, User user) {
        this.meeting = meeting;
        this.user = user;
    }

    public void attend() {
        this.status = Status.ATTEND;
    }

    public void close() {
        this.status = Status.ABSENT;
    }

    public boolean isPending() {
        return this.status == Status.PENDING;
    }

    public boolean isWrong(Integer code) {
        return !this.meeting.getCode().equals(code);
    }
}
