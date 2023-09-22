package io.openems.edge.energy.api.schedulable;

import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedule.Preset;

/**
 * Holds the Schedule for a {@link Schedulable} {@link Controller}.
 * 
 * @param <PRESET>         the type of the {@link Schedule.Preset}
 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
 */
public class Schedule<PRESET extends Preset, DYNAMIC_CONFIG> {

	/** Identifies a configuration Preset. */
	public static interface Preset {

		/**
		 * Gets this {@link Preset}s identification String.
		 *
		 * @return the identification String
		 */
		public String name();
	}

	/**
	 * Abstract Handler for delegation by {@link Schedulable} {@link Controller}s.
	 * 
	 * <p>
	 * Typically you will want to extend this class with your own "ScheduleHandler"
	 * to encapsulate all Schedule related implementations.
	 * 
	 * @param <STATIC_CONFIG>  the type of the static configuration
	 * @param <PRESET>         the type of the {@link Schedule.Preset}
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 */
	public abstract static class Handler<STATIC_CONFIG extends Annotation, PRESET extends Schedule.Preset, DYNAMIC_CONFIG> {

		public final PRESET[] presets;

		private final Logger log = LoggerFactory.getLogger(Handler.class);

		private Schedule<PRESET, DYNAMIC_CONFIG> schedule = null;
		private STATIC_CONFIG staticConfig = null;

		protected Handler(PRESET[] presets) {
			this.presets = presets;
		}

		/**
		 * Apply a statically configured Config.
		 * 
		 * <p>
		 * This method is typically called at Component Activated or Modified. If
		 * `config` is defined (i.e. != null), this `getCurrentConfig()` will always
		 * return this.
		 * 
		 * @param staticConfig the static configuration; or null
		 */
		public final void applyStaticConfig(STATIC_CONFIG staticConfig) {
			this.staticConfig = staticConfig;
		}

		/**
		 * Gets the static component configuration (provided via
		 * {@link #applyStaticConfig(Annotation)}.
		 * 
		 * @return the static component configuration
		 */
		public STATIC_CONFIG getStaticConfig() {
			return this.staticConfig;
		}

		/**
		 * Apply a {@link Schedule}.
		 * 
		 * @param schedule the {@link Schedule}
		 */
		@SuppressWarnings("unchecked")
		public final synchronized void applySchedule(Schedule<?, ?> schedule) {
			this.log.info("Apply new Schedule: \n" + schedule.toString());
			this.schedule = (Schedule<PRESET, DYNAMIC_CONFIG>) schedule;
		}

		/**
		 * Gets the current Config.
		 * 
		 * <p>
		 * Returns the Dynamic Config derived from Schedule Preset or from the static
		 * configuration.
		 * 
		 * @return the Config; never null
		 */
		public final synchronized DYNAMIC_CONFIG getCurrentConfig() {
			final PRESET preset;
			if (this.schedule == null) {
				preset = null;
			} else {
				preset = this.schedule.getCurrentPreset();
			}
			if (preset == null) {
				return this.toConfig(this.staticConfig);
			} else {
				return this.toConfig(this.staticConfig, preset);
			}
		}

		/**
		 * Creates a Dynamic Config from a a static component configuration.
		 * 
		 * @param config the static component configuration
		 * @return a Dynamic Config; never null
		 */
		protected abstract DYNAMIC_CONFIG toConfig(STATIC_CONFIG config);

		/**
		 * Creates a Dynamic Config from a Schedule Preset and a static component
		 * configuration.
		 * 
		 * @param config the static component configuration
		 * @param preset the Schedule Preset
		 * @return a Dynamic Config; never null
		 */
		protected abstract DYNAMIC_CONFIG toConfig(STATIC_CONFIG config, PRESET preset);
	}

	private final ImmutableSortedMap<ZonedDateTime, PRESET> schedule;

	/**
	 * Build empty {@link Schedule}.
	 * 
	 * @param <PRESET>         the type of the {@link Schedule.Preset}
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 * @return new {@link Schedule}
	 */
	public static <PRESET extends Preset, DYNAMIC_CONFIG> Schedule<PRESET, DYNAMIC_CONFIG> empty() {
		return new Schedule<>(ImmutableSortedMap.of());
	}

	/**
	 * Build a {@link Schedule} for hourly configuration PRESETs.
	 * 
	 * @param <PRESET>         the type of the {@link Schedule.Preset}
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 * @param fromDate         {@link ZonedDateTime} of the first preset
	 * @param presets          array of {@link Schedule.Preset}s, ideally 24 for one
	 *                         day
	 * @return new {@link Schedule}
	 */
	public static <PRESET extends Preset, DYNAMIC_CONFIG> Schedule<PRESET, DYNAMIC_CONFIG> ofHourly(
			ZonedDateTime fromDate, PRESET[] presets) {
		var b = ImmutableSortedMap.<ZonedDateTime, PRESET>naturalOrder();
		IntStream.range(0, presets.length) //
				.forEach(period -> {
					var time = fromDate.truncatedTo(ChronoUnit.HOURS).plusHours(period);
					b.put(time, presets[period]);
				});
		return new Schedule<>(b.build());
	}

	/**
	 * Build a {@link Schedule} for quarterly (15-minutes) configuration PRESETs.
	 * 
	 * @param <PRESET>         the type of the {@link Schedule.Preset}
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 * @param fromDate         {@link ZonedDateTime} of the first preset
	 * @param presets          array of {@link Schedule.Preset}s, ideally 24 for one
	 *                         day
	 * @return new {@link Schedule}
	 */
	public static <PRESET extends Preset, DYNAMIC_CONFIG> Schedule<PRESET, DYNAMIC_CONFIG> ofQuarterly(
			ZonedDateTime fromDate, PRESET[] presets) {
		var b = ImmutableSortedMap.<ZonedDateTime, PRESET>naturalOrder();
		IntStream.range(0, presets.length) //
				.forEach(period -> {
					var time = fromDate //
							.truncatedTo(ChronoUnit.HOURS).plusMinutes(15 * (fromDate.getMinute() / 15)) //
							.plusMinutes(period * 15);
					b.put(time, presets[period]);
				});
		return new Schedule<>(b.build());
	}

	public Schedule(ImmutableSortedMap<ZonedDateTime, PRESET> schedule) {
		this.schedule = schedule;
	}

	/**
	 * Gets the {@link Preset} for `ZonedDateTime.now()`.
	 * 
	 * @return the Preset, possibly null
	 */
	public PRESET getCurrentPreset() {
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
