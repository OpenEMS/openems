package io.openems.edge.controller.io.heating.room;

import java.time.Instant;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.jscalendar.JSCalendar.Task;

public class Utils {

	private Utils() {
	}

	protected static record HighPeriod(Instant from, Instant to) {
	}

	protected static HighPeriod getNextHighPeriod(ZonedDateTime now, ImmutableList<Task<JsonObject>> schedule) {
		return schedule.stream() //
				.map(task -> {
					var next = task.getNextOccurence(now);
					return new HighPeriod(next.toInstant(), next.plus(task.duration()).toInstant());
				}) //
				.sorted((t0, t1) -> t0.from.compareTo(t1.from)) //
				.findFirst() //
				.orElse(null);
	}
}
