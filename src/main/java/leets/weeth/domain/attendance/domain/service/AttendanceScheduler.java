package leets.weeth.domain.attendance.domain.service;

import jakarta.transaction.Transactional;
import java.util.List;
import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.schedule.domain.service.MeetingGetService;
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
        List<Meeting> meetings = meetingGetService.findAllOpenMeetingsBeforeNow();

        meetings.forEach(meeting -> {
                    meeting.close();
                    List<Attendance> attendanceList = attendanceGetService.findAllByMeeting(meeting);
                    attendanceUpdateService.close(attendanceList);
                });
    }
}
