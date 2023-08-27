package io.openems.edge.energy.api.schedulable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.schedulable.Schedule.Preset;

public class ScheduleHandler<CONFIG> {

	private final Logger log = LoggerFactory.getLogger(ScheduleHandler.class);

	public static class Builder<CONFIG> {

		private final ImmutableList.Builder<Preset<CONFIG>> presets = ImmutableList.builder();

		private Builder() {
		}

		/**
		 * Add a {@link Mode} with a config.
		 * 
		 * @param mode   the {@link Mode}
		 * @param config the config
		 * @return builder
		 */
		public Builder<CONFIG> add(Preset<CONFIG> preset) {
			this.presets.add(preset);
			return this;
		}

		public ScheduleHandler<CONFIG> build() {
			return new ScheduleHandler<CONFIG>(this.presets.build());
		}

	}

	/**
	 * Create a {@link ScheduleHandler} builder.
	 *
	 * @param <MODE>   the type of {@link Mode}
	 * @param <CONFIG> the type of Config
	 * @return a {@link Builder}
	 */
	public static <CONFIG> Builder<CONFIG> create() {
		return new Builder<CONFIG>();
	}

	/**
	 * Creates a {@link ScheduleHandler}.
	 * 
	 * @param <MODE>        the type of {@link Mode}
	 * @param <CONFIG>      the type of Config
	 * @param scheduleModes the array of Schedule-Modes
	 * @return new {@link ScheduleHandler}
	 */
	public static <CONFIG> ScheduleHandler<CONFIG> of(ImmutableList<? extends Preset<CONFIG>> presets) {
		return new ScheduleHandler<CONFIG>(presets);
	}

	/**
	 * Creates a {@link ScheduleHandler}.
	 * 
	 * @param <MODE>        the type of {@link Mode}
	 * @param <CONFIG>      the type of Config
	 * @param scheduleModes the array of Schedule-Modes
	 * @return new {@link ScheduleHandler}
	 */
	public static <CONFIG> ScheduleHandler<CONFIG> of(Preset<CONFIG>[] presets) {
		return new ScheduleHandler<CONFIG>(ImmutableList.copyOf(presets));
	}

	private final ImmutableList<? extends Preset<CONFIG>> presets;

	private Schedule<CONFIG> schedule = null;
	private CONFIG staticConfig = null;

	private ScheduleHandler(ImmutableList<? extends Preset<CONFIG>> presets) {
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
	public void applyConfig(CONFIG staticConfig) {
		this.staticConfig = staticConfig;
	}

	/**
	 * Apply a {@link Schedule}.
	 * 
	 * @param schedule the {@link Schedule}
	 */
	public synchronized void applySchedule(Schedule<CONFIG> schedule) {
		this.log.info("Apply new Schedule: ");
		this.log.info(schedule.toString());
		this.schedule = schedule;
	}

	/**
	 * Gets the current Config.
	 * 
	 * <p>
	 * If a static non-null config was provided via `applyConfig()`, this method
	 * always returns the static config. Otherwise gets the Config from the
	 * Schedule. If none exist, returns null.
	 * 
	 * @return the Config or null
	 */
	public synchronized CONFIG getCurrentConfig() {
		if (this.staticConfig != null) {
			return this.staticConfig;
		}
		if (this.schedule == null) {
			return null;
		}
		var preset = this.schedule.getCurrentPreset();
		if (preset == null) {
			return null;
		}
		return preset.getConfig();
	}

	public ImmutableList<? extends Preset<CONFIG>> getPresets() {
		return this.presets;
	}

}
