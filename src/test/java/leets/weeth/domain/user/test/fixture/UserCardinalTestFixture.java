package leets.weeth.domain.user.test.fixture;

import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;

public class UserCardinalTestFixture {

	public static UserCardinal linkUserCardinal(User user, Cardinal cardinal) {
		return new UserCardinal(user, cardinal);
	}
}
