package leets.weeth.domain.attendance.application.mapper;

import leets.weeth.domain.attendance.application.dto.AttendanceDTO;
import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.enums.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static leets.weeth.domain.attendance.test.fixture.AttendanceTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AttendanceMapperTest {

	@InjectMocks
	private final AttendanceMapper attendanceMapper = Mappers.getMapper(AttendanceMapper.class);

	@Test
	@DisplayName("toMainDto: 사용자 + 당일 출석 객체를 Main DTO로 매핑한다")
	void toMainDto_mapsUserAndTodayAttendance() {
		// given
		LocalDate today = LocalDate.now();
		Meeting meeting = createOneDayMeeting(today, 1, 1111, "Today");
		User user = createActiveUserWithAttendances("이지훈", List.of(meeting));
		Attendance attendance = user.getAttendances().get(0);

		// when
		AttendanceDTO.Main main = attendanceMapper.toMainDto(user, attendance);

		// then
		assertThat(main).isNotNull();
		assertThat(main.title()).isEqualTo("Today");
		assertThat(main.status()).isEqualTo(attendance.getStatus());
		assertThat(main.start()).isEqualTo(meeting.getStart());
		assertThat(main.end()).isEqualTo(meeting.getEnd());
		assertThat(main.location()).isEqualTo(meeting.getLocation());
	}

	@Test
	@DisplayName("toResponseDto: 단일 출석을 Response DTO로 매핑한다")
	void toResponseDto_mapsSingleAttendance() {
		// given
		Meeting meeting = createOneDayMeeting(LocalDate.now().minusDays(1), 1, 2222, "D-1");
		User user = createActiveUser("사용자A");
		Attendance attendance = createAttendance(meeting, user);

		// when
		AttendanceDTO.Response response = attendanceMapper.toResponseDto(attendance);

		// then
		assertThat(response).isNotNull();
		assertThat(response.title()).isEqualTo("D-1");
		assertThat(response.start()).isEqualTo(meeting.getStart());
		assertThat(response.end()).isEqualTo(meeting.getEnd());
		assertThat(response.location()).isEqualTo(meeting.getLocation());
	}

	@Test
	@DisplayName("toDetailDto: 사용자 + Response 리스트를 Detail DTO로 매핑(total = attend + absence)")
	void toDetailDto_mapsDetailAndTotal() {
		// given
		LocalDate base = LocalDate.now();
		Meeting m1 = createOneDayMeeting(base.minusDays(2), 1, 1000, "D-2");
		Meeting m2 = createOneDayMeeting(base.minusDays(1), 1, 1001, "D-1");
		User user = createActiveUser("이지훈");
		setUserAttendanceStats(user, 3, 2);

		Attendance a1 = createAttendance(m1, user);
		Attendance a2 = createAttendance(m2, user);

		AttendanceDTO.Response r1 = attendanceMapper.toResponseDto(a1);
		AttendanceDTO.Response r2 = attendanceMapper.toResponseDto(a2);

		// when
		AttendanceDTO.Detail detail = attendanceMapper.toDetailDto(user, List.of(r1, r2));

		// then
		assertThat(detail).isNotNull();
		assertThat(detail.attendances()).hasSize(2);
		assertThat(detail.total()).isEqualTo(5);
	}

	@Test
	@DisplayName("toAttendanceInfoDto: Attendance를 Info DTO로 매핑")
	void toAttendanceInfoDto_mapsInfo() {
		// given
		Meeting meeting = createOneDayMeeting(LocalDate.now(), 1, 3333, "Info");
		User user = createActiveUser("유저B");
		enrichUserProfile(user, Position.BE, "컴퓨터공학과", "20201234");

		Attendance attendance = createAttendance(meeting, user);
		setAttendanceId(attendance, 10L);

		// when
		AttendanceDTO.AttendanceInfo info = attendanceMapper.toAttendanceInfoDto(attendance);

		// then
		assertThat(info).isNotNull();
		assertThat(info.id()).isEqualTo(10L);
		assertThat(info.status()).isEqualTo(attendance.getStatus());
		assertThat(info.name()).isEqualTo("유저B");
	}

	@Test
	@DisplayName("null 안전성 테스트: todayAttendance가 null이면 필드는 null로 매핑")
	void nullSafety_whenTodayAttendanceNull() {
		// given
		User user = createActiveUser("이지훈");

		// when
		AttendanceDTO.Main main = attendanceMapper.toMainDto(user, null);

		// then
		assertThat(main).isNotNull();
		assertThat(main.title()).isNull();
		assertThat(main.start()).isNull();
		assertThat(main.end()).isNull();
		assertThat(main.location()).isNull();
	}

	@Test
	@DisplayName("toMainDto: 일반 유저는 출석 코드가 null로 매핑된다")
	void toMainDto_normalUser_codeIsNull() {
		// given
		LocalDate today = LocalDate.now();
		Meeting meeting = createOneDayMeeting(today, 1, 1234, "Today");
		User user = createActiveUserWithAttendances("일반유저", List.of(meeting));
		Attendance attendance = user.getAttendances().get(0);

		// when
		AttendanceDTO.Main main = attendanceMapper.toMainDto(user, attendance);

		// then
		assertThat(main).isNotNull();
		assertThat(main.code()).isNull();
		assertThat(main.title()).isEqualTo("Today");
		assertThat(main.status()).isEqualTo(attendance.getStatus());
	}

	@Test
	@DisplayName("toAdminResponse: ADMIN 유저는 출석 코드가 포함된다")
	void toAdminResponse_adminUser_includesCode() {
		// given
		LocalDate today = LocalDate.now();
		Integer expectedCode = 1234;
		Meeting meeting = createOneDayMeeting(today, 1, expectedCode, "Today");
		User adminUser = createAdminUserWithAttendances("관리자", List.of(meeting));
		Attendance attendance = adminUser.getAttendances().get(0);

		// when
		AttendanceDTO.Main main = attendanceMapper.toAdminResponse(adminUser, attendance);

		// then
		assertThat(main).isNotNull();
		assertThat(main.code()).isEqualTo(expectedCode);
		assertThat(main.title()).isEqualTo("Today");
		assertThat(main.start()).isEqualTo(meeting.getStart());
		assertThat(main.end()).isEqualTo(meeting.getEnd());
		assertThat(main.location()).isEqualTo(meeting.getLocation());
	}
}
