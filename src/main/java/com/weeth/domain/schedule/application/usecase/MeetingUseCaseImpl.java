package com.weeth.domain.schedule.application.usecase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.service.AttendanceDeleteService;
import com.weeth.domain.attendance.domain.service.AttendanceGetService;
import com.weeth.domain.attendance.domain.service.AttendanceSaveService;
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService;
import com.weeth.domain.schedule.application.dto.MeetingDTO;
import com.weeth.domain.schedule.application.dto.ScheduleDTO;
import com.weeth.domain.schedule.application.mapper.MeetingMapper;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.schedule.domain.service.MeetingDeleteService;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.schedule.domain.service.MeetingSaveService;
import com.weeth.domain.schedule.domain.service.MeetingUpdateService;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;
import com.weeth.domain.user.domain.repository.CardinalReader;
import com.weeth.domain.user.domain.repository.UserReader;
import com.weeth.domain.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

import static com.weeth.domain.schedule.application.dto.MeetingDTO.Response;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingUseCaseImpl implements MeetingUseCase {

    private final MeetingGetService meetingGetService;
    private final MeetingMapper mapper;
    private final MeetingSaveService meetingSaveService;
    private final UserReader userReader;
    private final UserRepository userRepository;
    private final MeetingUpdateService meetingUpdateService;
    private final MeetingDeleteService meetingDeleteService;
    private final AttendanceGetService attendanceGetService;
    private final AttendanceSaveService attendanceSaveService;
    private final AttendanceDeleteService attendanceDeleteService;
    private final AttendanceUpdateService attendanceUpdateService;
    private final CardinalReader cardinalReader;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Response find(Long userId, Long meetingId) {
        User user = userReader.getById(userId);
        Meeting meeting = meetingGetService.find(meetingId);

        if (Role.ADMIN == user.getRole()) {
            return mapper.toAdminResponse(meeting)  ;
        }

        return mapper.to(meeting);
    }

    @Override
    public MeetingDTO.Infos find(Integer cardinal) {
        List<Meeting> meetings;

        if (cardinal == null) {
            meetings = meetingGetService.findAll();
        } else {
            meetings = meetingGetService.findMeetingByCardinal(cardinal);
        }

        Meeting thisWeek = findThisWeek(meetings);
        List<Meeting> sorted = sortMeetings(meetings);

        return new MeetingDTO.Infos(
            thisWeek != null ? mapper.toInfo(thisWeek) : null,
            sorted.stream().map(mapper::toInfo).toList());
    }

    @Override
    @Transactional
    public void save(ScheduleDTO.Save dto, Long userId) {
        User user = userReader.getById(userId);
        Cardinal cardinal = cardinalReader.getByCardinalNumber(dto.cardinal());

        List<User> userList = userRepository.findAllByCardinalAndStatus(cardinal, Status.ACTIVE);

        Meeting meeting = mapper.from(dto, user);
        meetingSaveService.save(meeting);

        attendanceSaveService.saveAll(userList, meeting);
    }

    @Override
    @Transactional
    public void update(ScheduleDTO.Update dto, Long userId, Long meetingId) {
        Meeting meeting = meetingGetService.find(meetingId);
        User user = userReader.getById(userId);
        meetingUpdateService.update(dto, user, meeting);
    }

    @Override
    @Transactional
    public void delete(Long meetingId) {
        Meeting meeting = meetingGetService.find(meetingId);
        List<Attendance> attendances = attendanceGetService.findAllByMeeting(meeting);

        attendanceUpdateService.updateUserAttendanceByStatus(attendances);

        em.flush();
        em.clear();

        attendanceDeleteService.deleteAll(meeting);
        meetingDeleteService.delete(meeting);
    }

    private List<Meeting> sortMeetings(List<Meeting> meetings) {
        return meetings.stream()
            .sorted(Comparator.comparing(Meeting::getStart).reversed())
            .toList();
    }


    private Meeting findThisWeek(List<Meeting> meetings) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return meetings.stream()
            .filter(m -> {
                LocalDate d = m.getStart().toLocalDate();
                return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
            })
            .findFirst()
            .orElse(null);
    }

}
