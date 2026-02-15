package com.weeth.domain.user.domain.repository;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.weeth.config.TestContainersConfig;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.test.fixture.CardinalTestFixture;

@DataJpaTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CardinalRepositoryTest {

	@Autowired
	CardinalRepository cardinalRepository;

	@Test
	void 기수번호로_조회되는지() {
		//given
		var cardinal = CardinalTestFixture.createCardinal(7,2025,1);
		cardinalRepository.save(cardinal);

		//when
		var result = cardinalRepository.findByCardinalNumber(7);

		//then
		assertThat(result).isPresent();
		assertThat(result.get().getYear()).isEqualTo(2025);
	}


}
