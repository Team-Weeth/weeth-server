package leets.weeth.domain.attendance.application.usecase;

import leets.weeth.domain.attendance.application.dto.AttendanceDTO;
import leets.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;
import leets.weeth.domain.attendance.application.exception.AttendanceNotFoundException;
import leets.weeth.domain.attendance.application.mapper.AttendanceMapper;
import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.attendance.domain.entity.enums.Status;
import leets.weeth.domain.attendance.domain.service.AttendanceGetService;
import leets.weeth.domain.attendance.domain.service.AttendanceUpdateService;
import leets.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.schedule.domain.service.MeetingGetService;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.enums.Role;
import leets.weeth.domain.user.domain.service.UserCardinalGetService;
import leets.weeth.domain.user.domain.service.UserGetService;
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
                .filter(attendance -> attendance.getMeeting().getStart().minusMinutes(10).isBefore(now)
                        && attendance.getMeeting().getEnd().isAfter(now))
                .findAny()
                .orElseThrow(AttendanceNotFoundException::new);

        if (todayMeeting.isWrong(code))
            throw new AttendanceCodeMismatchException();

        if (todayMeeting.getStatus() != Status.ATTEND)
            attendanceUpdateService.attend(todayMeeting);
    }

    @Override
    public AttendanceDTO.Main find(Long userId) {
        User user = userGetService.find(userId);

        Attendance todayMeeting = user.getAttendances().stream()
                .filter(attendance -> attendance.getMeeting().getStart().toLocalDate().isEqual(LocalDate.now())
                        && attendance.getMeeting().getEnd().toLocalDate().isEqual(LocalDate.now()))
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
                .filter(attendance -> attendance.getMeeting().getCardinal().equals(currentCardinal.getCardinalNumber()))
                .sorted(Comparator.comparing(attendance -> attendance.getMeeting().getStart()))
                .map(mapper::toResponseDto)
                .toList();

        return mapper.toDetailDto(user, responses);
    }

    @Override
    public List<AttendanceDTO.AttendanceInfo> findAllAttendanceByMeeting(Long meetingId) {
        Meeting meeting = meetingGetService.find(meetingId);

        List<Attendance> attendances = attendanceGetService.findAllByMeeting(meeting);

        return attendances.stream()
                .map(mapper::toAttendanceInfoDto)
                .toList();
    }

    @Override
    public void close(LocalDate now, Integer cardinal) {
        List<Meeting> meetings = meetingGetService.find(cardinal);

        /*
        todo 차후 리팩토링 정기모임 id를 입력받아서 해당 정기모임의 출석을 마감하도록 수정
         */
        Meeting targetMeeting = meetings.stream()
                .filter(meeting -> meeting.getStart().toLocalDate().isEqual(now)
                        && meeting.getEnd().toLocalDate().isEqual(now))
                .findAny()
                .orElseThrow(MeetingNotFoundException::new);

        List<Attendance> attendanceList = attendanceGetService.findAllByMeeting(targetMeeting);

        attendanceUpdateService.close(attendanceList);
    }

    @Override
    @Transactional
    public void updateAttendanceStatus(List<AttendanceDTO.UpdateStatus> attendanceUpdates) {
        attendanceUpdates.forEach(update -> {
            Attendance attendance = attendanceGetService.findByAttendanceId(update.attendanceId());
            User user = attendance.getUser();

            Status newStatus = Status.valueOf(update.status());

            if (newStatus == Status.ABSENT) {
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
