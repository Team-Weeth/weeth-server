package com.weeth.domain.attendance.test.fixture;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.weeth.domain.attendance.domain.entity.Attendance;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Department;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;

public class AttendanceTestFixture {

	private AttendanceTestFixture() {}

	//todo : 추후 User Fixture 활용 예정
	public static User createActiveUser(String name) {
		return User.builder().name(name).status(Status.ACTIVE).build();
	}

	public static User createAdminUser(String name) {
		return User.builder().name(name).status(Status.ACTIVE).role(Role.ADMIN).build();
	}

	public static User createAdminUserWithAttendances(String name, List<Meeting> meetings) {
		User user = createAdminUser(name);

		if (user.getAttendances() == null) {
			try {
				java.lang.reflect.Field f = user.getClass().getDeclaredField("attendances");
				f.setAccessible(true);
				f.set(user, new java.util.ArrayList<>());
			} catch (Exception ignore) {}
		}
		if (meetings != null) {
			for (Meeting meeting : meetings) {
				Attendance attendance = createAttendance(meeting, user);
				user.add(attendance);
			}
		}
		return user;
	}

	//todo : 추후 User Fixture 활용 예정
	public static User createActiveUserWithAttendances(String name, List<Meeting> meetings) {
		User user = createActiveUser(name);

		if (user.getAttendances() == null) {
			try {
				java.lang.reflect.Field f = user.getClass().getDeclaredField("attendances");
				f.setAccessible(true);
				f.set(user, new java.util.ArrayList<>());
			} catch (Exception ignore) {}
		}
		if (meetings != null) {
			for (Meeting meeting : meetings) {
				Attendance attendance = createAttendance(meeting, user);
				user.add(attendance);
			}
		}
		return user;
	}

	public static Attendance createAttendance(Meeting meeting, User user) {
		return new Attendance(meeting, user);
	}

	public static Meeting createOneDayMeeting(LocalDate date, int cardinal, int code, String title) {
		return Meeting.builder()
			.title(title)
			.location("Test Location")
			.start(date.atTime(10, 0))
			.end(date.atTime(12, 0))
			.code(code)
			.cardinal(cardinal)
			.build();
	}

	public static Meeting createInProgressMeeting(int cardinal, int code, String title) {
		return Meeting.builder()
			.title(title)
			.location("Test Location")
			.start(LocalDateTime.now().minusMinutes(5))
			.end(LocalDateTime.now().plusMinutes(5))
			.code(code)
			.cardinal(cardinal)
			.build();
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setAttendanceId(Attendance attendance, Long id) {
		setField(attendance, "id", id);
	}

	public static void setUserAttendanceStats(User user, Integer attendanceCount, Integer absenceCount) {
		setField(user, "attendanceCount", attendanceCount);
		setField(user, "absenceCount", absenceCount);
	}

	public static void enrichUserProfile(User user, Position position, Department department, String studentId) {
		setField(user, "position", position);
		setField(user, "department", department);
		setField(user, "studentId", studentId);
	}

	public static void enrichUserProfile(User user, Position position, String departmentKoreanValue, String studentId) {
		setField(user, "position", position);
		Department department = Department.to(departmentKoreanValue); // ← 핵심
		setField(user, "department", department);
		setField(user, "studentId", studentId);
	}
}
