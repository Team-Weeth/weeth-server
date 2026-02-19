package com.weeth.domain.schedule.application.usecase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.Session;
import com.weeth.domain.attendance.domain.service.AttendanceDeleteService;
import com.weeth.domain.attendance.domain.service.AttendanceGetService;
import com.weeth.domain.attendance.domain.service.AttendanceSaveService;
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService;
import com.weeth.domain.schedule.application.dto.MeetingDTO;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.application.mapper.MeetingMapper;
import com.weeth.domain.schedule.domain.service.MeetingDeleteService;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.schedule.domain.service.MeetingSaveService;
import com.weeth.domain.schedule.domain.service.MeetingUpdateService;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

import static com.weeth.domain.schedule.application.dto.MeetingDTO.Info;
import static com.weeth.domain.schedule.application.dto.MeetingDTO.Response;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingUseCaseImpl implements MeetingUseCase {

    private final MeetingGetService meetingGetService;
    private final MeetingMapper mapper;
    private final MeetingSaveService meetingSaveService;
    private final UserGetService userGetService;
    private final MeetingUpdateService meetingUpdateService;
    private final MeetingDeleteService meetingDeleteService;
    private final AttendanceGetService attendanceGetService;
    private final AttendanceSaveService attendanceSaveService;
    private final AttendanceDeleteService attendanceDeleteService;
    private final AttendanceUpdateService attendanceUpdateService;
    private final CardinalGetService cardinalGetService;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Response find(Long userId, Long sessionId) {
        User user = userGetService.find(userId);
        Session session = meetingGetService.find(sessionId);

        if (Role.ADMIN == user.getRole()) {
            return mapper.toAdminResponse(session);
        }

        return mapper.to(session);
    }

    @Override
    public MeetingDTO.Infos find(Integer cardinal) {
        List<Session> sessions;

        if (cardinal == null) {
            sessions = meetingGetService.findAll();
        } else {
            sessions = meetingGetService.findMeetingByCardinal(cardinal);
        }

        Session thisWeek = findThisWeek(sessions);
        List<Session> sorted = sortSessions(sessions);

        return new MeetingDTO.Infos(
            thisWeek != null ? mapper.toInfo(thisWeek) : null,
            sorted.stream().map(mapper::toInfo).toList());
    }

    @Override
    @Transactional
    public void save(ScheduleDTO.Save dto, Long userId) {
        User user = userGetService.find(userId);
        Cardinal cardinal = cardinalGetService.findByUserSide(dto.cardinal());

        List<User> userList = userGetService.findAllByCardinal(cardinal);

        Session session = mapper.from(dto, user);
        meetingSaveService.save(session);

        attendanceSaveService.saveAll(userList, session);
    }

    @Override
    @Transactional
    public void update(ScheduleDTO.Update dto, Long userId, Long sessionId) {
        Session session = meetingGetService.find(sessionId);
        User user = userGetService.find(userId);
        meetingUpdateService.update(dto, user, session);
    }

    @Override
    @Transactional
    public void delete(Long sessionId) {
        Session session = meetingGetService.find(sessionId);
        List<Attendance> attendances = attendanceGetService.findAllByMeeting(session);

        attendanceUpdateService.updateUserAttendanceByStatus(attendances);

        em.flush();
        em.clear();

        attendanceDeleteService.deleteAll(session);
        meetingDeleteService.delete(session);
    }

    private List<Session> sortSessions(List<Session> sessions) {
        return sessions.stream()
            .sorted(Comparator.comparing(Session::getStart).reversed())
            .toList();
    }

    private Session findThisWeek(List<Session> sessions) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return sessions.stream()
            .filter(s -> {
                LocalDate d = s.getStart().toLocalDate();
                return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
            })
            .findFirst()
            .orElse(null);
    }
}
