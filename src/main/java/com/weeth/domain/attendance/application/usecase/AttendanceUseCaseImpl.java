package com.weeth.domain.attendance.application.usecase;

import com.weeth.domain.attendance.application.dto.AttendanceDTO;
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException;
import com.weeth.domain.attendance.application.mapper.AttendanceMapper;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus;
import com.weeth.domain.attendance.domain.service.AttendanceGetService;
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService;
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.service.UserCardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceUseCaseImpl implements AttendanceUseCase {

    private final UserGetService userGetService;
    private final UserCardinalGetService userCardinalGetService;

    private final AttendanceGetService attendanceGetService;
    private final AttendanceUpdateService attendanceUpdateService;
    private final AttendanceMapper mapper;

    private final MeetingGetService meetingGetService;

    @Override
    @Transactional
    public void checkIn(Long userId, Integer code) throws AttendanceCodeMismatchException {
        User user = userGetService.find(userId);

        LocalDateTime now = LocalDateTime.now();
        Attendance todayMeeting = user.getAttendances().stream()
                .filter(attendance -> attendance.getSession().getStart().minusMinutes(10).isBefore(now)
                        && attendance.getSession().getEnd().isAfter(now))
                .findAny()
                .orElseThrow(AttendanceNotFoundException::new);

        if (todayMeeting.isWrong(code))
            throw new AttendanceCodeMismatchException();

        if (todayMeeting.getStatus() != AttendanceStatus.ATTEND)
            attendanceUpdateService.attend(todayMeeting);
    }

    @Override
    public AttendanceDTO.Main find(Long userId) {
        User user = userGetService.find(userId);

        Attendance todayMeeting = user.getAttendances().stream()
                .filter(attendance -> attendance.getSession().getStart().toLocalDate().isEqual(LocalDate.now())
                        && attendance.getSession().getEnd().toLocalDate().isEqual(LocalDate.now()))
                .findAny()
                .orElse(null);

        if (Role.ADMIN == user.getRole()) {
            return mapper.toAdminResponse(user, todayMeeting);
        }

        return mapper.toMainDto(user, todayMeeting);
    }

    public AttendanceDTO.Detail findAllDetailsByCurrentCardinal(Long userId) {
        User user = userGetService.find(userId);
        Cardinal currentCardinal = userCardinalGetService.getCurrentCardinal(user);

        List<AttendanceDTO.Response> responses = user.getAttendances().stream()
                .filter(attendance -> attendance.getSession().getCardinal() == currentCardinal.getCardinalNumber())
                .sorted(Comparator.comparing(attendance -> attendance.getSession().getStart()))
                .map(mapper::toResponseDto)
                .toList();

        return mapper.toDetailDto(user, responses);
    }

    @Override
    public List<AttendanceDTO.AttendanceInfo> findAllAttendanceByMeeting(Long sessionId) {
        Session session = meetingGetService.find(sessionId);

        List<Attendance> attendances = attendanceGetService.findAllByMeeting(session);

        return attendances.stream()
                .map(mapper::toAttendanceInfoDto)
                .toList();
    }

    @Override
    public void close(LocalDate now, Integer cardinal) {
        List<Session> sessions = meetingGetService.find(cardinal);

        Session targetSession = sessions.stream()
                .filter(session -> session.getStart().toLocalDate().isEqual(now)
                        && session.getEnd().toLocalDate().isEqual(now))
                .findAny()
                .orElseThrow(MeetingNotFoundException::new);

        List<Attendance> attendanceList = attendanceGetService.findAllByMeeting(targetSession);

        attendanceUpdateService.close(attendanceList);
    }

    @Override
    @Transactional
    public void updateAttendanceStatus(List<AttendanceDTO.UpdateStatus> attendanceUpdates) {
        attendanceUpdates.forEach(update -> {
            Attendance attendance = attendanceGetService.findByAttendanceId(update.attendanceId());
            User user = attendance.getUser();

            AttendanceStatus newStatus = AttendanceStatus.valueOf(update.status());

            if (newStatus == AttendanceStatus.ABSENT) {
                attendance.close();
                user.removeAttend();
                user.absent();
            } else {
                attendance.attend();
                user.removeAbsent();
                user.attend();
            }
        });
    }
}
