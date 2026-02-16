package com.weeth.domain.schedule.domain.repository;

import com.weeth.domain.schedule.domain.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(LocalDateTime end, LocalDateTime start);

    List<Event> findAllByCardinal(int cardinal);
}
