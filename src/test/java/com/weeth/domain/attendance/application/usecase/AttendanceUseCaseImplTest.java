package com.weeth.domain.attendance.application.usecase;

import com.weeth.domain.attendance.application.dto.AttendanceDTO;
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException;
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException;
import com.weeth.domain.attendance.application.mapper.AttendanceMapper;
import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.attendance.domain.entity.enums.Status;
import com.weeth.domain.attendance.domain.service.AttendanceGetService;
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService;
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.service.UserCardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.weeth.domain.attendance.test.fixture.AttendanceTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceUseCaseImplTest {

	private final Long userId = 10L;
	@Mock private UserGetService userGetService;
	@Mock private UserCardinalGetService userCardinalGetService;
	@Mock private AttendanceGetService attendanceGetService;
	@Mock private AttendanceUpdateService attendanceUpdateService;
	@Mock private AttendanceMapper attendanceMapper;
	@Mock private MeetingGetService meetingGetService;
	@InjectMocks private AttendanceUseCaseImpl attendanceUseCase;

	@Test
	@DisplayName("find: 여러 날짜의 출석 목록 중 '시작/종료 날짜가 모두 오늘'인 출석정보를 선택")
	void find_todayMeeting_filtersDispersedAttendances() {
		// given
		LocalDate today = LocalDate.now();

		Meeting meetingYesterday = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday");
		Meeting meetingToday     = createOneDayMeeting(today,            1, 2222, "Today");
		Meeting meetingTomorrow  = createOneDayMeeting(today.plusDays(1),1, 3333, "Tomorrow");

		User user = createActiveUserWithAttendances(
			"이지훈", List.of(meetingYesterday, meetingToday, meetingTomorrow)
		);

		Attendance expectedTodayAttendance = user.getAttendances().stream()
			.filter(attendance -> "Today".equals(attendance.getMeeting().getTitle()))
			.findFirst()
			.orElseThrow();

		AttendanceDTO.Main mapped = mock(AttendanceDTO.Main.class);

		given(userGetService.find(userId)).willReturn(user);
		given(attendanceMapper.toMainDto(eq(user), eq(expectedTodayAttendance))).willReturn(mapped);

		// when
		AttendanceDTO.Main actual = attendanceUseCase.find(userId);

		// then
		assertThat(actual).isSameAs(mapped);
		then(attendanceMapper).should().toMainDto(eq(user), eq(expectedTodayAttendance));
		then(attendanceMapper).shouldHaveNoMoreInteractions();
	}

	@Test
	@DisplayName("10분 전부터 출석이 가능한지 확인")
	void checkIn_10MinutesBeforeMeeting_ShouldSucceed() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Meeting meeting = Meeting.builder()
				.start(now.plusMinutes(5))  // 5분 뒤 시작 (checkIn 로직의 '10분 전' 범위 내)
				.end(now.plusHours(2))
				.code(1234)
				.title("Today")
				.cardinal(1)
				.build();

		User user = createActiveUserWithAttendances(
				"이지훈", List.of(meeting)
		);

		when(userGetService.find(userId)).thenReturn(user);


		// when & then
		assertDoesNotThrow(() -> attendanceUseCase.checkIn(userId, 1234));
		verify(attendanceUpdateService, times(1)).attend(any(Attendance.class));
	}

	@Test
	@DisplayName("11분 전에 출석시 오류 확인")
	void checkIn_10MinutesBeforeMeeting_ShouldFailed() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Meeting meeting = Meeting.builder()
				.start(now.plusMinutes(11))  // 11분뒤 시작  -> 오류 발생해야함
				.end(now.plusHours(2))
				.code(1234)
				.title("Today")
				.cardinal(1)
				.build();

		User user = createActiveUserWithAttendances(
				"이지훈", List.of(meeting)
		);

		when(userGetService.find(userId)).thenReturn(user);


		// when & then
		assertThatThrownBy(() -> attendanceUseCase.checkIn(userId, 1234))
				.isInstanceOf(AttendanceNotFoundException.class);
	}
	@Test
	@DisplayName("find: '시작/종료 날짜가 모두 오늘'인 출석이 없다면, mapper.toMainDto(user, null)을 호출")
	void find_noExactToday_returnsNullMapped() {
		// given
		LocalDate today = LocalDate.now();

		Meeting yesterdayMeeting = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday");
		Meeting tomorrowMeeting  = createOneDayMeeting(today.plusDays(1), 1, 3333, "Tomorrow");

		User user = createActiveUserWithAttendances("이지훈",
			List.of(yesterdayMeeting, tomorrowMeeting));

		when(userGetService.find(userId)).thenReturn(user);
		AttendanceDTO.Main mapped = mock(AttendanceDTO.Main.class);
		when(attendanceMapper.toMainDto(user, null)).thenReturn(mapped);

		// when
		AttendanceDTO.Main actual = attendanceUseCase.find(userId);

		// then
		assertThat(actual).isSameAs(mapped);
		verify(attendanceMapper).toMainDto(user, null);
	}

	@Test
	@DisplayName("findAllDetailsByCurrentCardinal: 현재 기수만 필터링·정렬하여 Detail 매핑")
	void findAllDetailsByCurrentCardinal() {
		// given
		LocalDate today = LocalDate.now();
		Meeting meetingDayMinus1 = createOneDayMeeting(today.minusDays(1), 1, 1111, "D-1");
		Meeting meetingToday = createOneDayMeeting(today, 1, 2222, "D-Day");
		User user = createActiveUserWithAttendances("이지훈", List.of(meetingDayMinus1, meetingToday));

		List<Attendance> userAttendances = user.getAttendances();
		Attendance attendanceFirst = userAttendances.get(0);   // D-1
		Attendance attendanceSecond = userAttendances.get(1);  // D-Day

		when(userGetService.find(userId)).thenReturn(user);
		Cardinal currentCardinal = mock(Cardinal.class);
		when(currentCardinal.getCardinalNumber()).thenReturn(1);
		when(userCardinalGetService.getCurrentCardinal(user)).thenReturn(currentCardinal);

		AttendanceDTO.Response responseFirst = mock(AttendanceDTO.Response.class);
		AttendanceDTO.Response responseSecond = mock(AttendanceDTO.Response.class);
		when(attendanceMapper.toResponseDto(attendanceFirst)).thenReturn(responseFirst);
		when(attendanceMapper.toResponseDto(attendanceSecond)).thenReturn(responseSecond);

		AttendanceDTO.Detail expectedDetail = mock(AttendanceDTO.Detail.class);
		when(attendanceMapper.toDetailDto(eq(user), anyList())).thenReturn(expectedDetail);

		// when
		AttendanceDTO.Detail actualDetail = attendanceUseCase.findAllDetailsByCurrentCardinal(userId);

		// then
		assertThat(actualDetail).isSameAs(expectedDetail);
		verify(attendanceMapper).toDetailDto(eq(user), argThat(list -> list.size() == 2));
	}

	@Test
	@DisplayName("close(now, cardinal): 당일 정기모임을 찾아 close")
	void close_success() {
		// given
		LocalDate now = LocalDate.now();
		Meeting targetMeeting = createOneDayMeeting(now, 1, 1111, "Today");
		Meeting otherMeeting  = createOneDayMeeting(now.minusDays(1), 1, 9999, "Yesterday");

		Attendance attendance1 = mock(Attendance.class);
		Attendance attendance2 = mock(Attendance.class);

		when(meetingGetService.find(1)).thenReturn(List.of(targetMeeting, otherMeeting));
		when(attendanceGetService.findAllByMeeting(targetMeeting)).thenReturn(List.of(attendance1, attendance2));

		// when
		attendanceUseCase.close(now, 1);

		// then
		verify(attendanceUpdateService).close(argThat(list ->
			list.size() == 2 && list.containsAll(List.of(attendance1, attendance2))
		));
	}

	@Test
	@DisplayName("close(now, cardinal): 당일 정기모임이 없으면 MeetingNotFoundException")
	void close_notFound() {
		// given
		LocalDate now = LocalDate.now();
		Meeting otherDayMeeting = createOneDayMeeting(now.minusDays(1), 1, 9999, "Yesterday");

		when(meetingGetService.find(1)).thenReturn(List.of(otherDayMeeting));

		// when & then
		assertThatThrownBy(() -> attendanceUseCase.close(now, 1))
			.isInstanceOf(MeetingNotFoundException.class);
	}

	@Nested
	@DisplayName("checkIn")
	class CheckInTest {

		@Test
		@DisplayName("진행 중 정기모임이고 코드 일치하며 상태가 ATTEND가 아니면 출석 처리")
		void checkIn_success() {
			// given
			User user = mock(User.class);
			Meeting inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress");
			Attendance attendance = mock(Attendance.class);
			when(attendance.getMeeting()).thenReturn(inProgressMeeting);
			when(attendance.isWrong(1234)).thenReturn(false);
			when(attendance.getStatus()).thenReturn(Status.PENDING);

			when(userGetService.find(userId)).thenReturn(user);
			when(user.getAttendances()).thenReturn(List.of(attendance));

			// when
			attendanceUseCase.checkIn(userId, 1234);

			// then
			verify(attendanceUpdateService).attend(attendance);
		}

		@Test
		@DisplayName("진행 중 정기모임이 없으면 AttendanceNotFoundException")
		void checkIn_notFoundMeeting() {
			// given
			User user = mock(User.class);
			when(userGetService.find(userId)).thenReturn(user);
			when(user.getAttendances()).thenReturn(List.of());

			// when & then
			assertThatThrownBy(() -> attendanceUseCase.checkIn(userId, 1234))
				.isInstanceOf(AttendanceNotFoundException.class);
		}

		@Test
		@DisplayName("코드 불일치 시 AttendanceCodeMismatchException")
		void checkIn_wrongCode() {
			// given
			User user = mock(User.class);
			Meeting inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress");

			Attendance attendance = mock(Attendance.class);
			when(attendance.getMeeting()).thenReturn(inProgressMeeting);
			when(attendance.isWrong(9999)).thenReturn(true);

			when(userGetService.find(userId)).thenReturn(user);
			when(user.getAttendances()).thenReturn(List.of(attendance));

			// when & then
			assertThatThrownBy(() -> attendanceUseCase.checkIn(userId, 9999))
				.isInstanceOf(AttendanceCodeMismatchException.class);
		}

		@Test
		@DisplayName("이미 ATTEND면 추가 처리 없이 종료")
		void checkIn_alreadyAttend() {
			// given
			User user = mock(User.class);
			Meeting inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress");

			Attendance attendance = mock(Attendance.class);
			when(attendance.getMeeting()).thenReturn(inProgressMeeting);
			when(attendance.isWrong(1234)).thenReturn(false);
			when(attendance.getStatus()).thenReturn(Status.ATTEND);

			when(userGetService.find(userId)).thenReturn(user);
			when(user.getAttendances()).thenReturn(List.of(attendance));

			// when
			attendanceUseCase.checkIn(userId, 1234);

			// then
			verify(attendanceUpdateService, never()).attend(any());
		}
	}
}
