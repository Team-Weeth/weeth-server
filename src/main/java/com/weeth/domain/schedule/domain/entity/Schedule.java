package com.weeth.domain.schedule.domain.entity;

import jakarta.persistence.*;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String location;

    private Integer cardinal;

    private String requiredItem;

    private LocalDateTime start;

    private LocalDateTime end;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public void updateUpperClass(ScheduleDTO.Update dto, User user) {
        this.title = dto.title();
        this.content = dto.content();
        this.location = dto.location();
        this.requiredItem = dto.requiredItem();
        this.start = dto.start();
        this.end = dto.end();
        this.user = user;
    }
}
