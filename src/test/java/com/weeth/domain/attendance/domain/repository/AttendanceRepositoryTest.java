package com.weeth.domain.attendance.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.weeth.config.TestContainersConfig;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus;
import com.weeth.domain.schedule.domain.repository.MeetingRepository;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Status;
import com.weeth.domain.user.domain.repository.UserRepository;

@DataJpaTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AttendanceRepositoryTest {

	@Autowired private AttendanceRepository attendanceRepository;
	@Autowired private MeetingRepository meetingRepository;
	@Autowired private UserRepository userRepository;

	private Meeting meeting;
	private User activeUser1;
	private User activeUser2;

	@BeforeEach
	void setUp() {
		meeting = Meeting.builder()
			.title("1차 정기모임")
			.start(LocalDateTime.now().minusHours(1))
			.end(LocalDateTime.now().plusHours(1))
			.code(1234)
			.cardinal(1)
			.meetingStatus(MeetingStatus.OPEN)
			.build();
		meetingRepository.save(meeting);

		activeUser1 = User.builder().name("이지훈").status(Status.ACTIVE).build();
		activeUser2 = User.builder().name("이강혁").status(Status.ACTIVE).build();
		userRepository.saveAll(List.of(activeUser1, activeUser2));

		attendanceRepository.save(new Attendance(meeting, activeUser1));
		attendanceRepository.save(new Attendance(meeting, activeUser2));
	}

	@Test
	@DisplayName("특정 정기모임 + 사용자 상태로 출석 목록 조회")
	void findAllByMeetingAndUserStatus() {
		// given
		Status active = activeUser1.getStatus();

		// when
		List<Attendance> attendances =
			attendanceRepository.findAllByMeetingAndUserStatus(meeting, active);

		// then
		assertThat(attendances).hasSize(2);
		assertThat(attendances)
			.extracting(a -> a.getUser().getName())
			.containsExactlyInAnyOrder("이지훈", "이강혁");
	}

	@Test
	@DisplayName("특정 정기모임의 모든 출석 레코드 삭제")
	void deleteAllByMeeting() {
		// when
		attendanceRepository.deleteAllByMeeting(meeting);

		// then
		List<Attendance> after = attendanceRepository.findAll();
		assertThat(after).isEmpty();
	}
}
