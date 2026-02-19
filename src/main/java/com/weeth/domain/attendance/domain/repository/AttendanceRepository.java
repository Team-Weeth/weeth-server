package com.weeth.domain.attendance.domain.repository;

import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.user.domain.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllBySessionAndUserStatus(Session session, Status status);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.session = :session")
    void deleteAllBySession(Session session);
}
