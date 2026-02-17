package com.weeth.domain.attendance.application.usecase;

import java.util.List;
import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest;
import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse;
import com.weeth.domain.attendance.application.dto.response.AttendanceInfoResponse;
import com.weeth.domain.attendance.application.dto.response.AttendanceMainResponse;
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;

import java.time.LocalDate;

public interface AttendanceUseCase {
    void checkIn(Long userId, Integer code) throws AttendanceCodeMismatchException;

    AttendanceMainResponse find(Long userId);

    AttendanceDetailResponse findAllDetailsByCurrentCardinal(Long userId);

    List<AttendanceInfoResponse> findAllAttendanceByMeeting(Long meetingId);

    void close(LocalDate now, Integer cardinal);

    void updateAttendanceStatus(List<UpdateAttendanceStatusRequest> attendanceUpdates);
}
