package com.weeth.domain.attendance.domain.service;

import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceDeleteService {

    private final AttendanceRepository attendanceRepository;

    public void deleteAll(Session session) {
        attendanceRepository.deleteAllBySession(session);
    }
}
