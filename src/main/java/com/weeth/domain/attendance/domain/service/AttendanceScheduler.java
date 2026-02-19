package com.weeth.domain.attendance.domain.service;

import jakarta.transaction.Transactional;
import java.util.List;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final MeetingGetService meetingGetService;
    private final AttendanceGetService attendanceGetService;
    private final AttendanceUpdateService attendanceUpdateService;

    @Transactional
    @Scheduled(cron = "0 0 22 * * THU", zone = "Asia/Seoul")
    public void autoCloseAttendance() {
        List<Session> sessions = meetingGetService.findAllOpenMeetingsBeforeNow();

        sessions.forEach(session -> {
            session.close();
            List<Attendance> attendanceList = attendanceGetService.findAllByMeeting(session);
            attendanceUpdateService.close(attendanceList);
        });
    }
}
