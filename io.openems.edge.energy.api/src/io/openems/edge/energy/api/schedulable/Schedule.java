package io.openems.edge.energy.api.schedulable;

import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.Utils;
import io.openems.edge.energy.api.schedulable.Schedule.Preset;

/**
 * Holds the Schedule for a {@link Schedulable} {@link Controller}.
 * 
 * @param <PRESET>         the configuration preset type
 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
 */
public class Schedule<PRESET extends Preset, DYNAMIC_CONFIG> {

	/**
	 * Identifies a configuration Preset.
	 */
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
	 * @param <PRESET>
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 */
	public static abstract class Handler<STATIC_CONFIG extends Annotation, PRESET extends Schedule.Preset, DYNAMIC_CONFIG> {

		public final Preset[] presets;

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
		 * @param config the static configuration; or null
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
			return staticConfig;
		}

		/**
		 * Apply a {@link Schedule}.
		 * 
		 * @param schedule the {@link Schedule}
		 */
		public final synchronized void applySchedule(Schedule<PRESET, DYNAMIC_CONFIG> schedule) {
			this.log.info("Apply new Schedule: ");
			this.log.info(schedule.toString());
			this.schedule = schedule;
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
				return toConfig(this.staticConfig, preset);
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
	 * @param <PRESET>         the configuration preset type
	 * @param <DYNAMIC_CONFIG> the type of the dynamic configuration
	 * @return new {@link Schedule}
	 */
	public static <PRESET extends Preset, DYNAMIC_CONFIG> Schedule<PRESET, DYNAMIC_CONFIG> empty() {
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
	public static <PRESET extends Preset, DYNAMIC_CONFIG> Schedule<PRESET, DYNAMIC_CONFIG> of(ZonedDateTime now,
			PRESET[] presets) {
		var b = ImmutableSortedMap.<ZonedDateTime, PRESET>naturalOrder();
		IntStream.range(0, presets.length) //
				.forEach(period -> {
					b.put(Utils.toZonedDateTime(now, period), presets[period]);
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
