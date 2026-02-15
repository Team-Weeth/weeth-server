package com.weeth.domain.schedule.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus;
import com.weeth.domain.user.domain.entity.User;
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
public class Meeting extends Schedule {

    private Integer code;

    @Enumerated(EnumType.STRING)
    private MeetingStatus meetingStatus;

    public void update(ScheduleDTO.Update dto, User user) {
        this.updateUpperClass(dto, user);
    }

    @PrePersist
    public void init() {
        this.meetingStatus = MeetingStatus.OPEN;
    }

    public void close() {
        this.meetingStatus = MeetingStatus.CLOSE;
    }
}
