package leets.weeth.domain.user.domain.repository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import leets.weeth.config.TestContainersConfig;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import leets.weeth.domain.user.domain.entity.enums.Status;
import leets.weeth.domain.user.test.fixture.CardinalTestFixture;
import leets.weeth.domain.user.test.fixture.UserCardinalTestFixture;
import leets.weeth.domain.user.test.fixture.UserTestFixture;

@DataJpaTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCardinalRepository userCardinalRepository;

	@Autowired
	private CardinalRepository cardinalRepository;

	private Cardinal cardinal7;
	private Cardinal cardinal8;

	@BeforeEach
	void setUp() {

		cardinal7 = cardinalRepository.save(CardinalTestFixture.createCardinal(7, 2026, 1));
		cardinal8 = cardinalRepository.save(CardinalTestFixture.createCardinal(8, 2026, 2));

		var user1 = userRepository.save(UserTestFixture.createActiveUser1());
		var user2 = userRepository.save(UserTestFixture.createActiveUser2());
		var user3 = userRepository.save(UserTestFixture.createWaitingUser1());

		user1.accept();
		user2.accept();
		userCardinalRepository.flush();

		userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user1, cardinal7));
		userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user2, cardinal8));
		userCardinalRepository.save(UserCardinalTestFixture.linkUserCardinal(user3, cardinal7));

	}

	@Test
	@DisplayName("findAllByCardinalAndStatus(): 특정 기수 + 상태에 맞는 유저만 조회된다")
	void findAllByCardinalAndStatus() {
		// when
		List<User> result = userRepository.findAllByCardinalAndStatus(cardinal7, Status.ACTIVE);

		// then
		assertThat(result)
			.hasSize(1)
			.extracting(User::getName)
			.containsExactly("적순");
	}

	@Test
	@DisplayName("findAllByStatusOrderByCardinalAndName() : 상태별로 최신 기수순 + 이름 오름차순으로 정렬된다")
	void findAllByStatusOrderByCardinalAndName() {
		//given
		Pageable pageable = PageRequest.of(0,10);

		//when
		Slice<User> resultSlice = userRepository.findAllByStatusOrderedByCardinalAndName(Status.ACTIVE, pageable);
		List<User> result = resultSlice.getContent();

		//then
		assertThat(result)
			.hasSize(2)
			.extracting(User::getName)
			.containsExactly("적순2", "적순");
	}

	@Test
	@DisplayName("findAllByCardinalOrderByNameAsc() : Active인 유저들 중 특정 기수 + 이름 오름차순으로 정렬한다.")
	void findAllByCardinalOrderByNameAsc() {
		//given
		Pageable pageable = PageRequest.of(0,10);

		//when
		Slice<User> resultSlice = userRepository.findAllByCardinalOrderByNameAsc(Status.ACTIVE, cardinal7,pageable);
		List<User> result = resultSlice.getContent();

		//then
		assertThat(result)
			.hasSize(1)
			.extracting(User::getName)
			.containsExactly("적순");
	}

}
