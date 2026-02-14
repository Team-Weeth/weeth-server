package leets.weeth.domain.user.domain.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import leets.weeth.domain.user.application.exception.CardinalNotFoundException;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import leets.weeth.domain.user.domain.repository.UserCardinalRepository;
import leets.weeth.domain.user.test.fixture.CardinalTestFixture;
import leets.weeth.domain.user.test.fixture.UserTestFixture;

@ExtendWith(MockitoExtension.class)
public class UserCardinalGetServiceTest {

	@Mock
	private UserCardinalRepository userCardinalRepository;

	@InjectMocks
	private UserCardinalGetService userCardinalGetService;


	@Test
	@DisplayName("notContains() : 유저의 기수 목록 중, 특정 기수가 없는지 확인 ")
	void notContains() {
		//given
		var user = UserTestFixture.createActiveUser1();
		var existingCardinal = CardinalTestFixture.createCardinal(7,2025,2);
		var targetCardinal = CardinalTestFixture.createCardinal(8,2026,1);
		var userCardinal = new UserCardinal(user, existingCardinal);

		given(userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user))
			.willReturn(List.of(userCardinal));

		//when
		boolean result = userCardinalGetService.notContains(user, targetCardinal);

		//then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("isCurrent() : 현재 유저의 최신 기수보다 최신 기수인지 확인")
	void isCurrent() {
		//given
		var user = UserTestFixture.createActiveUser1();
		var oldCardinal = CardinalTestFixture.createCardinal(7,2025,2);
		var newCardinal = CardinalTestFixture.createCardinal(8,2026,1);
		var userCardinal = new UserCardinal(user, oldCardinal);

		given(userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user))
			.willReturn(List.of(userCardinal));

		//when
		boolean result = userCardinalGetService.isCurrent(user, newCardinal);

		//then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("isCurrent(): 새 기수가 기존 최대보다 작으면 false 반환")
	void isCurrent_returnsFalse_whenOlderCardinal() {
		// given
		var user = UserTestFixture.createActiveUser1();
		var oldCardinal = CardinalTestFixture.createCardinal(7, 2025, 1);
		var newCardinal = CardinalTestFixture.createCardinal(6, 2024, 2);
		var userCardinal = new UserCardinal(user, oldCardinal);

		given(userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user))
			.willReturn(List.of(userCardinal));

		// when
		boolean result = userCardinalGetService.isCurrent(user, newCardinal);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("isCurrent(): 유저가 어떤 기수도 가지고 있지 않으면 CardinalNotFoundException 발생")
	void isCurrent_throwsException_whenUserHasNoCardinal() {
		// given
		var user = UserTestFixture.createActiveUser1();
		var newCardinal = CardinalTestFixture.createCardinal(8, 2026, 1);

		given(userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user))
			.willReturn(List.of());

		// when & then
		assertThatThrownBy(() -> userCardinalGetService.isCurrent(user, newCardinal))
			.isInstanceOf(CardinalNotFoundException.class);
	}

}
