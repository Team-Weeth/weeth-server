package com.weeth.domain.user.application.usecase;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.weeth.domain.attendance.domain.service.AttendanceSaveService;
import com.weeth.domain.schedule.domain.entity.Meeting;
import com.weeth.domain.schedule.domain.service.MeetingGetService;
import com.weeth.domain.user.application.dto.request.UserRequestDto;
import com.weeth.domain.user.application.dto.response.UserResponseDto;
import com.weeth.domain.user.application.exception.InvalidUserOrderException;
import com.weeth.domain.user.application.mapper.UserMapper;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.UserCardinal;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.UserCardinalGetService;
import com.weeth.domain.user.domain.service.UserCardinalSaveService;
import com.weeth.domain.user.domain.service.UserDeleteService;
import com.weeth.domain.user.domain.service.UserGetService;
import com.weeth.domain.user.domain.service.UserUpdateService;
import com.weeth.domain.user.test.fixture.CardinalTestFixture;
import com.weeth.domain.user.test.fixture.UserTestFixture;
import com.weeth.global.auth.jwt.service.JwtRedisService;

@ExtendWith(MockitoExtension.class)
public class UserManageUseCaseTest {

	@Mock private UserGetService userGetService;
	@Mock private UserUpdateService userUpdateService;
	@Mock private UserDeleteService userDeleteService;

	@Mock private AttendanceSaveService attendanceSaveService;
	@Mock private MeetingGetService meetingGetService;
	@Mock private JwtRedisService jwtRedisService;
	@Mock private CardinalGetService cardinalGetService;
	@Mock private UserCardinalSaveService userCardinalSaveService;
	@Mock private UserCardinalGetService userCardinalGetService;

	@Mock private UserMapper userMapper;
	@Mock private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserManageUseCaseImpl useCase;


	@Test
	void findAllByAdmin_orderBy가_null이면_예외가정상발생하는지(){
		//given
		UsersOrderBy orderBy = null;

		//when & then
		assertThatThrownBy(() -> useCase.findAllByAdmin(orderBy))
			.isInstanceOf(InvalidUserOrderException.class);

	}

	@Test
	void findAllByAdmin이_orderBy에_맞게_정렬되어_조회되는지() {
		//given
		UsersOrderBy orderBy = UsersOrderBy.NAME_ASCENDING;

		var user1 = UserTestFixture.createActiveUser1();
		var user2 = UserTestFixture.createWaitingUser2();
		var cd1 = CardinalTestFixture.createCardinal(1L,6,2020,2);
		var cd2 = CardinalTestFixture.createCardinal(2L,7,2021,1);
		var uc1 = new UserCardinal(user1, cd1);
		var uc2 = new UserCardinal(user2, cd2);

		var adminResponse1 = new UserResponseDto.AdminResponse(
			1, "aaa", "a@a.com", "202034420", "01011112222", "산업공학과",
			List.of(6), null, Status.ACTIVE, null,
			0, 0, 0, 0, 0,
			LocalDateTime.now().minusDays(3),
			LocalDateTime.now()
		);

		var adminResponse2 = new UserResponseDto.AdminResponse(
			2, "bbb", "b@b.com", "202045678", "01033334444", "컴퓨터공학과",
			List.of(7), null, Status.WAITING, null,
			0, 0, 0, 0, 0,
			LocalDateTime.now().minusDays(2),
			LocalDateTime.now()
		);

		given(userCardinalGetService.getUserCardinals(user1)).willReturn(List.of(uc1));
		given(userCardinalGetService.getUserCardinals(user2)).willReturn(List.of(uc2));
		given(userCardinalGetService.findAll()).willReturn(List.of(uc2, uc1));
		given(userMapper.toAdminResponse(user1, List.of(uc1))).willReturn(adminResponse1);
		given(userMapper.toAdminResponse(user2, List.of(uc2))).willReturn(adminResponse2);


		//when
		var result = useCase.findAllByAdmin(UsersOrderBy.NAME_ASCENDING);


		//then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).name()).isEqualTo("aaa");
		assertThat(result.get(1).name()).isEqualTo("bbb");

	}


	@Test
	void accept_비활성유저_승인시_출석초기화_정상호출되는지() {
		//given
		var user1 = UserTestFixture.createWaitingUser1(1L);
		var userIds = new UserRequestDto.UserId(List.of(1L));
		var cardinal = CardinalTestFixture.createCardinal(1L,8,2020,2);
		var meetings = List.of(mock(Meeting.class));

		given(userGetService.findAll(userIds.userId())).willReturn(List.of(user1));
		given(userCardinalGetService.getCurrentCardinal(user1)).willReturn(cardinal);
		given(meetingGetService.find(8)).willReturn(meetings);

		//when
		useCase.accept(userIds);

		//then
		then(userUpdateService).should().accept(user1);
		then(attendanceSaveService).should().init(user1,meetings);

	}

	@Test
	void update_유저권한변경시_DB와_Redis_모두갱신되는지() {
		// given
		var user1 = UserTestFixture.createActiveUser1(1L);
		var request = new UserRequestDto.UserRoleUpdate(1L, Role.ADMIN);

		lenient().when(userGetService.find((Long)1L)).thenReturn(user1);

		// when
		useCase.update(List.of(request));

		// then
		then(userUpdateService).should().update(user1, "ADMIN");
		then(jwtRedisService).should().updateRole(1L, "ADMIN");
	}

	@Test
	void leave_회원탈퇴시_토큰무효화_및_유저상태변경되는지() {
		//given
		var user1 = UserTestFixture.createActiveUser1(1L);
		given(userGetService.find((Long)1L)).willReturn(user1);

		//when
		useCase.leave(1L);

		//then
		then(jwtRedisService).should().delete(1L);
		then(userDeleteService).should().leave(user1);

	}

	@Test
	void ban_회원ban시_토큰무효화_및_유저상태변경되는지() {
		//given
		var user1 = UserTestFixture.createActiveUser1(1L);
		var ids = new UserRequestDto.UserId(List.of(1L));
		given(userGetService.findAll(ids.userId())).willReturn(List.of(user1));

		//when
		useCase.ban(ids);

		//then
		then(jwtRedisService).should().delete(1L);
		then(userDeleteService).should().ban(user1);

	}

	@Test
	void applyOB_현재기수_OB신청시_출석초기화_및_기수업데이트() {
		//given
		var user = User.builder().id(1L).name("aaa").status(Status.ACTIVE).attendances(new ArrayList<>()).build();
		var nextCardinal = CardinalTestFixture.createCardinal(1L,4,2020,2);
		var request = new UserRequestDto.UserApplyOB(1L,4);
		var meeting = List.of(mock(Meeting.class));

		given(userGetService.find((Long)1L)).willReturn(user);
		given(cardinalGetService.findByAdminSide(4)).willReturn(nextCardinal);
		given(userCardinalGetService.notContains(user, nextCardinal)).willReturn(true);
		given(userCardinalGetService.isCurrent(user, nextCardinal)).willReturn(true);
		given(meetingGetService.find(4)).willReturn(meeting);

		//when
		useCase.applyOB(List.of(request));

		//then
		then(attendanceSaveService).should().init(user,meeting);
		then(userCardinalSaveService).should().save(any(UserCardinal.class));
	}

	@Test
	void reset_비밀번호초기화시_모든유저에_reset호출되는지() {
		// given
		var user1 = UserTestFixture.createActiveUser1(1L);
		var user2 = UserTestFixture.createActiveUser2(2L);
		var ids = new UserRequestDto.UserId(List.of(1L, 2L));

		given(userGetService.findAll(ids.userId())).willReturn(List.of(user1, user2));

		// when
		useCase.reset(ids);

		// then
		then(userGetService).should().findAll(ids.userId());
		then(userUpdateService).should().reset(user1, passwordEncoder);
		then(userUpdateService).should().reset(user2, passwordEncoder);
	}

}
