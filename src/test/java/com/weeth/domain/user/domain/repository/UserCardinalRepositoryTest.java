package com.weeth.domain.user.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.weeth.config.TestContainersConfig;
import com.weeth.domain.user.domain.entity.UserCardinal;
import com.weeth.domain.user.test.fixture.CardinalTestFixture;
import com.weeth.domain.user.test.fixture.UserTestFixture;

@DataJpaTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserCardinalRepositoryTest {

	@Autowired
	UserRepository userRepository;

	@Autowired
	CardinalRepository cardinalRepository;

	@Autowired
	UserCardinalRepository userCardinalRepository;

	@Test
	void 유저별_기수_내림차순_조회되는지() {
		//given
		var user = UserTestFixture.createActiveUser1();

		userRepository.save(user);

		var cardinal1 = cardinalRepository.save(CardinalTestFixture.createCardinal(5,2023,1));
		var cardinal2 = cardinalRepository.save(CardinalTestFixture.createCardinal(6,2023,2));
		var cardinal3 = cardinalRepository.save(CardinalTestFixture.createCardinal(7,2024,1));

		userCardinalRepository.saveAll(List.of(
			new UserCardinal(user, cardinal1),
			new UserCardinal(user, cardinal2),
			new UserCardinal(user, cardinal3)
		));

		//when
		List<UserCardinal> result = userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user);

		//then
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getCardinal().getCardinalNumber()).isEqualTo(7);
		assertThat(result.get(1).getCardinal().getCardinalNumber()).isEqualTo(6);
		assertThat(result.get(2).getCardinal().getCardinalNumber()).isEqualTo(5);

	}

	@Test
	void 여러_유저의_기수를_유저별_내림차순으로_조회한다() {
		//given
		var user1 = UserTestFixture.createActiveUser1();
		var user2 = UserTestFixture.createActiveUser2();

		userRepository.save(user1);
		userRepository.save(user2);

		var c1 = cardinalRepository.save(CardinalTestFixture.createCardinal(5,2023,1));
		var c2 = cardinalRepository.save(CardinalTestFixture.createCardinal(6,2023,2));
		var c3 = cardinalRepository.save(CardinalTestFixture.createCardinal(7,2024,1));
		var c4 = cardinalRepository.save(CardinalTestFixture.createCardinal(8,2024,2));

		userCardinalRepository.saveAll(List.of(
			new UserCardinal(user1, c3),
			new UserCardinal(user1, c2)
		));
		userCardinalRepository.saveAll(List.of(
			new UserCardinal(user2, c4),
			new UserCardinal(user2, c1)
		));

		//when
		List<UserCardinal> result = userCardinalRepository.findAllByUsers(List.of(user1, user2));

		//then
		assertThat(result).hasSize(4);
		assertThat(result.get(0).getUser().getId()).isEqualTo(user1.getId());
		assertThat(result.get(0).getCardinal().getCardinalNumber()).isEqualTo(7);
		assertThat(result.get(1).getCardinal().getCardinalNumber()).isEqualTo(6);

		assertThat(result.get(2).getUser().getId()).isEqualTo(user2.getId());
		assertThat(result.get(2).getCardinal().getCardinalNumber()).isEqualTo(8);
		assertThat(result.get(3).getCardinal().getCardinalNumber()).isEqualTo(5);
	}


}
