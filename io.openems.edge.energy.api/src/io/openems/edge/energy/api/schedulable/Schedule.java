package io.openems.edge.energy.api.schedulable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.Utils;

public class Schedule<CONFIG> {

	/**
	 * Holds a predefined Config.
	 * 
	 * @param <CONFIG> the type of Config
	 */
	public static interface Preset<CONFIG> {

		/**
		 * Gets this {@link Mode}s String representation.
		 *
		 * @return the String representation
		 */
		public String name();

		/**
		 * The actual Config.
		 * 
		 * @return config
		 */
		public CONFIG getConfig();
	}

	private final ImmutableSortedMap<ZonedDateTime, ? extends Preset<CONFIG>> schedule;

	/**
	 * Build empty {@link Schedule}.
	 * 
	 * @param <MODE> the {@link Mode} type
	 * @return new {@link Schedule}
	 */
	public static <CONFIG> Schedule<CONFIG> empty() {
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
	public static <CONFIG> Schedule<CONFIG> of(ZonedDateTime now, List<? extends Preset<CONFIG>> presets) {
		var b = ImmutableSortedMap.<ZonedDateTime, Preset<CONFIG>>naturalOrder();
		IntStream.range(0, presets.size()) //
				.forEach(period -> {
					b.put(Utils.toZonedDateTime(now, period), presets.get(period));
				});
		return new Schedule<>(b.build());
	}

	public Schedule(ImmutableSortedMap<ZonedDateTime, ? extends Preset<CONFIG>> schedule) {
		this.schedule = schedule;
	}

	/**
	 * Gets the {@link Preset} for `ZonedDateTime.now()`.
	 * 
	 * @return the Preset, possibly null
	 */
	public Preset<CONFIG> getCurrentPreset() {
		var entry = this.schedule.floorEntry(ZonedDateTime.now());
		if (entry == null) {
			return null;
		}
		return entry.getValue();
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
