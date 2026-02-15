package com.weeth.domain.user.test.fixture;

import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;

public class UserTestFixture {

	public static User createActiveUser1() {
		return User.builder()
			.name("적순")
			.email("test1@test.com")
			.status(Status.ACTIVE)
			.build();
	}

	public static User createActiveUser1(Long id) {
		return User.builder()
			.id(id)
			.name("적순")
			.email("test1@test.com")
			.status(Status.ACTIVE)
			.build();
	}

	public static User createActiveUser2() {
		return User.builder()
			.name("적순2")
			.email("test2@test.com")
			.status(Status.ACTIVE)
			.build();
	}

	public static User createActiveUser2(Long id) {
		return User.builder()
			.id(id)
			.name("적순2")
			.email("test2@test.com")
			.status(Status.ACTIVE)
			.build();
	}

	public static User createWaitingUser1() {
		return User.builder()
			.name("순적")
			.email("test2@test.com")
			.status(Status.WAITING)
			.build();
	}

	public static User createWaitingUser1(Long id) {
		return User.builder()
			.id(id)
			.name("순적")
			.email("test2@test.com")
			.status(Status.WAITING)
			.build();
	}

	public static User createWaitingUser2() {
		return User.builder()
			.name("순적2")
			.email("test3@test.com")
			.status(Status.WAITING)
			.build();
	}

	public static User createWaitingUser2(Long id) {
		return User.builder()
			.id(id)
			.name("순적2")
			.email("test3@test.com")
			.status(Status.WAITING)
			.build();
	}

    public static User createAdmin(Long id) {
        return User.builder()
                .id(id)
                .name("적순")
                .email("admin@test.com")
                .status(Status.ACTIVE)
                .role(Role.ADMIN)
                .build();
    }

}
