package com.weeth.domain.schedule.domain.entity;

import jakarta.persistence.Entity;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.user.domain.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Event extends Schedule {

    public void update(ScheduleDTO.Update dto, User user) {
        this.updateUpperClass(dto, user);
    }
}
