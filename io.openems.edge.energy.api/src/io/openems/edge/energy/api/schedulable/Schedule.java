package io.openems.edge.energy.api.schedulable;

import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.Utils;

public class Schedule<MODE extends Schedule.Mode> {

	public static interface Mode {

		/**
		 * Gets this {@link Mode}s String representation.
		 *
		 * @return the String representation
		 */
		public String name();
	}

	// CHECKSTYLE:OFF
	public static interface ModeConfig<MODE extends Schedule.Mode, CONFIG> extends Mode {
		// CHECKSTYLE:ON

		/**
		 * The actual Config.
		 * 
		 * @return config
		 */
		public CONFIG getModeConfig();
	}

	private final ImmutableSortedMap<ZonedDateTime, MODE> schedule;

	/**
	 * Build empty {@link Schedule}.
	 * 
	 * @param <MODE> the {@link Mode} type
	 * @return new {@link Schedule}
	 */
	public static <MODE extends Schedule.Mode> Schedule<MODE> empty() {
		return new Schedule<>(ImmutableSortedMap.of());
	}

	/**
	 * Build {@link Schedule}.
	 * 
	 * @param <MODE> the {@link Mode} type
	 * @param now    {@link ZonedDateTime} of now
	 * @param modes  array of {@link Mode}s
	 * @return new {@link Schedule}
	 */
	public static <MODE extends Schedule.Mode> Schedule<MODE> of(ZonedDateTime now, MODE[] modes) {
		var b = ImmutableSortedMap.<ZonedDateTime, MODE>naturalOrder();
		IntStream.range(0, modes.length) //
				.forEach(period -> {
					b.put(Utils.toZonedDateTime(now, period), modes[period]);
				});
		return new Schedule<>(b.build());
	}

	public Schedule(ImmutableSortedMap<ZonedDateTime, MODE> schedule) {
		this.schedule = schedule;
	}

	/**
	 * Gets the {@link ScheduleMode} for `ZonedDateTime.now()`.
	 * 
	 * @return the Mode, possibly null
	 */
	public MODE getCurrentMode() {
		var mode = this.schedule.floorEntry(ZonedDateTime.now());
		if (mode == null) {
			return null;
		}
		return mode.getValue();
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		this.schedule.forEach((t, m) -> {
			b.append(t.toString());
			b.append(": ");
			b.append(m.name());
			b.append("\n");
		});
		return b.toString();
	}

}
