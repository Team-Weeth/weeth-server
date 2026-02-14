package leets.weeth.domain.attendance.domain.service;

import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.attendance.domain.repository.AttendanceRepository;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceDeleteService {

    private final AttendanceRepository attendanceRepository;

    public void deleteAll(Meeting meeting) {
        attendanceRepository.deleteAllByMeeting(meeting);
    }
}
