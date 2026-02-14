package leets.weeth.domain.user.domain.entity;

import jakarta.persistence.*;
import leets.weeth.domain.user.application.dto.request.CardinalUpdateRequest;
import leets.weeth.domain.user.domain.entity.enums.CardinalStatus;
import leets.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Cardinal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cardinal_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer cardinalNumber;

    private Integer year;

    private Integer semester;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    CardinalStatus status = CardinalStatus.DONE;

    public void update(CardinalUpdateRequest dto) {
        this.year = dto.year();
        this.semester = dto.semester();
    }

    public void inProgress() {
        this.status = CardinalStatus.IN_PROGRESS;
    }

    public void done() {
        this.status = CardinalStatus.DONE;
    }

}
