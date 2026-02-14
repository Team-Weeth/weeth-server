package leets.weeth.domain.user.application.usecase;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import leets.weeth.domain.user.application.dto.request.CardinalSaveRequest;
import leets.weeth.domain.user.application.dto.request.CardinalUpdateRequest;
import leets.weeth.domain.user.application.dto.response.CardinalResponse;
import leets.weeth.domain.user.application.mapper.CardinalMapper;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.enums.CardinalStatus;
import leets.weeth.domain.user.domain.service.CardinalGetService;
import leets.weeth.domain.user.domain.service.CardinalSaveService;
import leets.weeth.domain.user.test.fixture.CardinalTestFixture;

@ExtendWith(MockitoExtension.class)
public class CardinalUseCaseTest {
	// 실제 CardinalUseCase에서 사용하는 의존성을 Mock 객체로 대신 주입
	@Mock
	private CardinalGetService cardinalGetService;

	@Mock
	private CardinalSaveService cardinalSaveService;

	@Mock
	private CardinalMapper cardinalMapper;

	@InjectMocks
	private CardinalUseCase useCase;

	// Given-When-Then 패턴을 쉽게 이해하기 위해 메서드명_상황_예상결과로  테스트 메서드 네이밍


	@Test // 진행중이 아닌 기수를 등록하는 경우
	void save_진행중이_아닌_기수라면_검증후_저장만() {
		//given
		var request = new CardinalSaveRequest(7, 2025,1,false);

		var toSave = CardinalTestFixture.createCardinal(7, 2025, 1);
		var saved =  CardinalTestFixture.createCardinal(7,2025 ,1);

		willDoNothing().given(cardinalGetService).validateCardinal(7);
		given(cardinalMapper.from(request)).willReturn(toSave);
		given(cardinalSaveService.save(toSave)).willReturn(saved);

		//when
		useCase.save(request);

		//then
		then(cardinalGetService).should().validateCardinal(7);
		then(cardinalSaveService).should().save(toSave);
		then(cardinalGetService).should(never()).findInProgress();
	}

	@Test
	void save_새_기수가_진행중이라면_기존_기수는_done_현재기수는_inProgress() {
		// given
		var request = new CardinalSaveRequest(7, 2025,1,true);

		var oldCardinal = CardinalTestFixture.createCardinalInProgress(6, 2024, 2);
		var newCardinalBeforeSave = CardinalTestFixture.createCardinal(7, 2025, 1);
		var newCardinalAfterSave = CardinalTestFixture.createCardinal(7, 2025, 1);

		given(cardinalGetService.findInProgress()).willReturn(List.of(oldCardinal));
		given(cardinalMapper.from(request)).willReturn(newCardinalBeforeSave);
		given(cardinalSaveService.save(newCardinalBeforeSave)).willReturn(newCardinalAfterSave);

		// when
		useCase.save(request);

		// then
		then(cardinalGetService).should().findInProgress();
		then(cardinalSaveService).should().save(newCardinalBeforeSave);

		assertThat(oldCardinal.getStatus()).isEqualTo(CardinalStatus.DONE);
		assertThat(newCardinalAfterSave.getStatus()).isEqualTo(CardinalStatus.IN_PROGRESS);
	}


	@Test
	void update_연도와_학기를_변경한다() {
		//given
		var cardinal = CardinalTestFixture.createCardinal(6, 2024, 2);
		var dto = new CardinalUpdateRequest(1L, 2025,1,false);

		//when
		cardinal.update(dto);

		//then
		assertThat(cardinal.getYear()).isEqualTo(2025);
		assertThat(cardinal.getSemester()).isEqualTo(1);
	}


	@Test
	void findAll_조회된_모든_기수를_DTO로_매핑처리() {

		//given
		var cardinal1 = CardinalTestFixture.createCardinal(1L,6,2024,2);
		var cardinal2 = CardinalTestFixture.createCardinalInProgress(2L,7,2025,1);
		var cardinals = List.of(cardinal1, cardinal2);
		var now = LocalDateTime.now();

		var response1 = new CardinalResponse(
			1L, 6, 2024, 2,
			CardinalStatus.DONE,
			now.minusDays(5),
			now.minusDays(3)
		);

		var response2 = new CardinalResponse(
			2L, 7, 2025, 1,
			CardinalStatus.IN_PROGRESS,
			now.minusDays(2),
			now
		);

		given(cardinalGetService.findAll()).willReturn(cardinals);
		given(cardinalMapper.to(cardinal1)).willReturn(response1);
		given(cardinalMapper.to(cardinal2)).willReturn(response2);

		//when
		List<CardinalResponse> responses = useCase.findAll();


		//then
		then(cardinalGetService).should().findAll();
		then(cardinalMapper).should(times(2)).to(any(Cardinal.class));

		assertThat(responses)
			.asInstanceOf(list(CardinalResponse.class))
			.hasSize(2)
			.extracting(CardinalResponse::cardinalNumber)
			.containsExactly(6, 7);

		assertThat(responses)
			.asInstanceOf(list(CardinalResponse.class))
			.extracting(CardinalResponse::status)
			.containsExactly(CardinalStatus.DONE, CardinalStatus.IN_PROGRESS);

		assertThat(responses.get(0).createdAt()).isBefore(responses.get(1).createdAt());
	}

}
