package io.openems.edge.controller.io.heating.room;

import java.time.Instant;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;

public class Utils {

	private Utils() {
	}

	protected static record HighPeriod(Instant from, Instant to) {
	}

	protected static HighPeriod getNextHighPeriod(ZonedDateTime now, ImmutableList<Task<JsonObject>> schedule) {
		return JSCalendar.Tasks.getNextOccurence(schedule, now) //
				.map(ot -> new HighPeriod(ot.start().toInstant(), ot.start().plus(ot.duration()).toInstant())) //
				.orElse(null);
	}
}
