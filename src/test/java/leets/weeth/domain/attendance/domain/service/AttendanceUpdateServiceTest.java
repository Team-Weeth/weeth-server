package leets.weeth.domain.attendance.domain.service;

import static leets.weeth.domain.attendance.test.fixture.AttendanceTestFixture.*;
import static leets.weeth.domain.schedule.test.fixture.ScheduleTestFixture.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import leets.weeth.domain.attendance.domain.entity.Attendance;
import leets.weeth.domain.attendance.domain.entity.enums.Status;
import leets.weeth.domain.schedule.domain.entity.Meeting;
import leets.weeth.domain.user.domain.entity.User;

class AttendanceUpdateServiceTest {

	private final AttendanceUpdateService attendanceUpdateService = new AttendanceUpdateService();

	@Test
	@DisplayName("attend(): attendance.attend() + user.attend() 호출")
	void attend_callsEntityMethods() {
		// given
		Meeting meeting = createMeeting();
		User realUser = createActiveUser("이지훈");
		User userSpy = spy(realUser);

		doNothing().when(userSpy).attend();

		Attendance realAttendance = createAttendance(meeting, userSpy);
		Attendance attendanceSpy = spy(realAttendance);

		// when
		attendanceUpdateService.attend(attendanceSpy);

		// then
		verify(attendanceSpy).attend();
		verify(userSpy).attend();
	}

	@Test
	@DisplayName("close(): pending만 close() + user.absent() 호출")
	void close_onlyPendingIsClosed() {
		// given
		Meeting meeting = createMeeting();

		User pendingUserReal = createActiveUser("pending-user");
		User nonPendingUserReal = createActiveUser("non-pending-user");
		User pendingUserSpy = spy(pendingUserReal);
		User nonPendingUserSpy = spy(nonPendingUserReal);

		doNothing().when(pendingUserSpy).absent();
		doNothing().when(nonPendingUserSpy).absent();

		Attendance pendingAttendanceReal = createAttendance(meeting, pendingUserSpy);
		Attendance nonPendingAttendanceReal = createAttendance(meeting, nonPendingUserSpy);
		Attendance pendingAttendanceSpy = spy(pendingAttendanceReal);
		Attendance nonPendingAttendanceSpy = spy(nonPendingAttendanceReal);

		doReturn(true).when(pendingAttendanceSpy).isPending();
		doReturn(false).when(nonPendingAttendanceSpy).isPending();

		// when
		attendanceUpdateService.close(List.of(pendingAttendanceSpy, nonPendingAttendanceSpy));

		// then
		verify(pendingAttendanceSpy).close();
		verify(pendingUserSpy).absent();

		verify(nonPendingAttendanceSpy, never()).close();
		verify(nonPendingUserSpy, never()).absent();
	}

	@Test
	@DisplayName("updateUserAttendanceByStatus: ATTEND면 user.removeAttend(), 그 외에는 user.removeAbsent()")
	void updateUserAttendanceByStatus() {
		// given
		Meeting meeting = createMeeting();

		User attendUserReal = createActiveUser("attend-user");
		User absentUserReal = createActiveUser("absent-user");

		User attendUserSpy = spy(attendUserReal);
		User absentUserSpy = spy(absentUserReal);

		doNothing().when(attendUserSpy).removeAttend();
		doNothing().when(absentUserSpy).removeAbsent();

		Attendance attendAttendanceReal = createAttendance(meeting, attendUserSpy);
		Attendance absentAttendanceReal = createAttendance(meeting, absentUserSpy);
		Attendance attendAttendanceSpy = spy(attendAttendanceReal);
		Attendance absentAttendanceSpy = spy(absentAttendanceReal);

		doReturn(Status.ATTEND).when(attendAttendanceSpy).getStatus();
		doReturn(Status.ABSENT).when(absentAttendanceSpy).getStatus();
		doReturn(attendUserSpy).when(attendAttendanceSpy).getUser();
		doReturn(absentUserSpy).when(absentAttendanceSpy).getUser();

		// when
		attendanceUpdateService.updateUserAttendanceByStatus(List.of(attendAttendanceSpy, absentAttendanceSpy));

		// then
		verify(attendUserSpy).removeAttend();
		verify(absentUserSpy).removeAbsent();
	}
}
