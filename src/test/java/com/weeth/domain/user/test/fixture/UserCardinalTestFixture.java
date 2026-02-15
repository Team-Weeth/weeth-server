package com.weeth.domain.user.test.fixture;

import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.UserCardinal;

public class UserCardinalTestFixture {

	public static UserCardinal linkUserCardinal(User user, Cardinal cardinal) {
		return new UserCardinal(user, cardinal);
	}
}
