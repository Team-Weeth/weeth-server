package leets.weeth.domain.schedule.test.fixture;

import java.time.LocalDateTime;

import leets.weeth.domain.schedule.domain.entity.Event;
import leets.weeth.domain.schedule.domain.entity.Meeting;

public class ScheduleTestFixture {

	public static Event createEvent() {
		return Event.builder()
			.title("Test Meeting")
			.location("Test Location")
			.start(LocalDateTime.now())
			.end(LocalDateTime.now().plusDays(2))
			.cardinal(1)
			.build();
	}

	public static Meeting createMeeting() {
		return Meeting.builder()
			.title("Test Meeting")
			.location("Test Location")
			.start(LocalDateTime.now())
			.end(LocalDateTime.now().plusDays(2))
			.code(1234)
			.cardinal(1)
			.build();
	}
}
