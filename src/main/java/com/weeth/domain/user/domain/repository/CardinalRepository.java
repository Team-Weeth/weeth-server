package com.weeth.domain.user.domain.repository;

import java.util.List;
import java.util.Optional;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.enums.CardinalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardinalRepository extends JpaRepository<Cardinal, Long> {

    Optional<Cardinal> findByCardinalNumber(Integer cardinal);

    Optional<Cardinal> findByYearAndSemester(Integer year, Integer semester);

    List<Cardinal> findAllByStatus(CardinalStatus cardinalStatus);

    Cardinal findFirstByStatusOrderByCardinalNumberDesc(CardinalStatus status);

    List<Cardinal> findAllByOrderByCardinalNumberAsc();

    List<Cardinal> findAllByOrderByCardinalNumberDesc();
}
