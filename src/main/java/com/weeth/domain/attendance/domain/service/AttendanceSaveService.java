package com.weeth.domain.attendance.domain.service;

import jakarta.transaction.Transactional;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.repository.AttendanceRepository;
import com.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceSaveService {

    private final AttendanceRepository attendanceRepository;

    public void init(User user, List<Session> sessions) {
        if (sessions != null) {
            sessions.forEach(session -> {
                Attendance attendance = attendanceRepository.save(Attendance.Companion.create(session, user));
                user.add(attendance);
            });
        }
    }

    public void saveAll(List<User> userList, Session session) {
        List<Attendance> attendances = userList.stream()
                .map(user -> Attendance.Companion.create(session, user))
                .toList();

        attendanceRepository.saveAll(attendances);
    }
}
