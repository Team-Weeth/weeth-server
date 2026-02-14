package leets.weeth.domain.penalty.domain.repository;

import leets.weeth.domain.penalty.domain.entity.Penalty;
import leets.weeth.domain.penalty.domain.entity.enums.PenaltyType;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByUserIdAndCardinalIdOrderByIdDesc(Long userId, Long cardinalId);

    Optional<Penalty> findFirstByUserAndCardinalAndPenaltyTypeAndCreatedAtAfterOrderByCreatedAtAsc(
            User user, Cardinal cardinal, PenaltyType penaltyType, LocalDateTime createdAt);

    List<Penalty> findByCardinalIdOrderByIdDesc(Long cardinalId);
}
