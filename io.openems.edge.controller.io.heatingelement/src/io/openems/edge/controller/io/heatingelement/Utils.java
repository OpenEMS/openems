package io.openems.edge.controller.io.heatingelement;

import java.time.Instant;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;

public class Utils {

	private Utils() {
	}

	protected static record HighPeriod(Instant from, Instant to, Integer payload) {
	}

	protected static HighPeriod getNextHighPeriod(ZonedDateTime now, ImmutableList<Task<Payload>> schedule) {
		return JSCalendar.Tasks.getNextOccurence(schedule, now).map(ot -> {
			Integer payload = null;
			if (ot.payload() != null) {
				payload = ot.payload().sessionEnergy();
			}
			return new HighPeriod(ot.start().toInstant(), ot.start().plus(ot.duration()).toInstant(), payload);
		}).orElse(null);
	}
}
