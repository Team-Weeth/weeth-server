package leets.weeth.domain.user.domain.entity.enums;

import leets.weeth.domain.user.application.exception.StatusNotFoundException;
import lombok.Getter;

@Getter
public enum StatusPriority {
	ACTIVE(1),
	WAITING(2),
	LEFT(3),
	BANNED(4);

	private final int priority;

	StatusPriority(int priority) {
		this.priority = priority;
	}

	public static StatusPriority fromStatus(Status status) {
		if (status == null) {
			throw new StatusNotFoundException();
		}
		return StatusPriority.valueOf(status.name());
	}
}
