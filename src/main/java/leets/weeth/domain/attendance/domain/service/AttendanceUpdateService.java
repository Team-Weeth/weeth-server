package leets.weeth.domain.attendance.domain.service;

import jakarta.transaction.Transactional;
import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.attendance.domain.entity.enums.Status;
import leets.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceUpdateService {

    public void attend(Attendance attendance) {
        attendance.attend();
        attendance.getUser().attend();
    }

    public void close(List<Attendance> attendances) {
        attendances.stream()
                .filter(Attendance::isPending)
                .forEach(attendance -> {
                    attendance.close();
                    attendance.getUser().absent();
                });
    }

    public void updateUserAttendanceByStatus(List<Attendance> attendances) {
        for (Attendance attendance : attendances) {
            User user = attendance.getUser();
            if (attendance.getStatus().equals(Status.ATTEND)) {
                user.removeAttend();
            } else {
                user.removeAbsent();
            }
        }
    }
}
