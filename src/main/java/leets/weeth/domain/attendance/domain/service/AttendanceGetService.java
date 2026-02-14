package leets.weeth.domain.attendance.domain.service;

import leets.weeth.domain.attendance.application.exception.AttendanceNotFoundException;
import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.attendance.domain.repository.AttendanceRepository;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.user.domain.entity.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceGetService {

    private final AttendanceRepository attendanceRepository;

    public List<Attendance> findAllByMeeting(Meeting meeting) {
        return attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE);
    }
    public Attendance findByAttendanceId(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(AttendanceNotFoundException::new);
    }
}
