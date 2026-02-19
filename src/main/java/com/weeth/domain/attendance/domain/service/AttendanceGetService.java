package com.weeth.domain.attendance.domain.service;

import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.repository.AttendanceRepository;
import com.weeth.domain.user.domain.entity.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceGetService {

    private final AttendanceRepository attendanceRepository;

    public List<Attendance> findAllByMeeting(Session session) {
        return attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE);
    }

    public Attendance findByAttendanceId(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(AttendanceNotFoundException::new);
    }
}
