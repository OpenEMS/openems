package io.openems.edge.controller.evse.single;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class Utils {

	protected static final int FORCE_CHARGE_POWER = 11000; // [W]
	protected static final int MIN_CHARGE_POWER = 4600; // [W]

	private Utils() {
	}

	protected static ZonedDateTime getTargetDateTime(ZonedDateTime startTime, int hour) {
		var localTime = startTime.withZoneSameInstant(Clock.systemDefaultZone().getZone());
		var targetDate = localTime.getHour() > hour //
				? startTime.plusDays(1) //
				: startTime;
		return targetDate.truncatedTo(ChronoUnit.DAYS).withHour(hour);
	}
}
