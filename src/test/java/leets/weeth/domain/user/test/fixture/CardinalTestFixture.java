package leets.weeth.domain.user.test.fixture;

import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.enums.CardinalStatus;

public class CardinalTestFixture {

	public static Cardinal createCardinal(int cardinalNumber, int year, int semester) {
		return Cardinal.builder()
			.cardinalNumber(cardinalNumber)
			.year(year)
			.semester(semester)
			.status(CardinalStatus.DONE)
			.build();
	}

	public static Cardinal createCardinal(Long id, int cardinalNumber, int year, int semester) {
		return Cardinal.builder()
			.id(id)
			.cardinalNumber(cardinalNumber)
			.year(year)
			.semester(semester)
			.status(CardinalStatus.DONE)
			.build();
	}

	public static Cardinal createCardinalInProgress ( int cardinalNumber, int year, int semester) {
		return Cardinal.builder()
			.cardinalNumber(cardinalNumber)
			.year(year)
			.semester(semester)
			.status(CardinalStatus.IN_PROGRESS)
			.build();
	}

	public static Cardinal createCardinalInProgress(Long id, int cardinalNumber, int year, int semester) {
		return Cardinal.builder()
			.id(id)
			.cardinalNumber(cardinalNumber)
			.year(year)
			.semester(semester)
			.status(CardinalStatus.IN_PROGRESS)
			.build();
	}
}
