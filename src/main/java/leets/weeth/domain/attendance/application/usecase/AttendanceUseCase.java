package leets.weeth.domain.attendance.application.usecase;

import java.util.List;
import leets.weeth.domain.attendance.application.dto.AttendanceDTO;
import leets.weeth.domain.attendance.application.dto.AttendanceDTO.AttendanceInfo;
import leets.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;

import java.time.LocalDate;

import static leets.weeth.domain.attendance.application.dto.AttendanceDTO.Detail;
import static leets.weeth.domain.attendance.application.dto.AttendanceDTO.Main;

public interface AttendanceUseCase {
    void checkIn(Long userId, Integer code) throws AttendanceCodeMismatchException;

    Main find(Long userId);

    Detail findAllDetailsByCurrentCardinal(Long userId);

    List<AttendanceInfo> findAllAttendanceByMeeting(Long meetingId);

    void close(LocalDate now, Integer cardinal);

    void updateAttendanceStatus(List<AttendanceDTO.UpdateStatus> attendanceUpdates);
}
