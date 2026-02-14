package leets.weeth.domain.user.domain.entity;

import jakarta.persistence.*;
import leets.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class UserCardinal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_cardinal_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "cardinal_id")
    private Cardinal cardinal;

    public UserCardinal(User user, Cardinal cardinal) {
        this.user = user;
        this.cardinal = cardinal;
    }
}
