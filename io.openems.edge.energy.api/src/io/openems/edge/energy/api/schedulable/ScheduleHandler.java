package io.openems.edge.energy.api.schedulable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.openems.edge.energy.api.schedulable.Schedule.Mode;

public class ScheduleHandler<MODE extends Schedule.Mode, CONFIG> {

	private final Logger log = LoggerFactory.getLogger(ScheduleHandler.class);

	public static class Builder<MODE extends Schedule.Mode, CONFIG> {

		private final ImmutableMap.Builder<MODE, CONFIG> configs = ImmutableMap.builder();

		private Builder() {
		}

		/**
		 * Add a {@link Mode} with a config.
		 * 
		 * @param mode   the {@link Mode}
		 * @param config the config
		 * @return builder
		 */
		public Builder<MODE, CONFIG> add(MODE mode, CONFIG config) {
			this.configs.put(mode, config);
			return this;
		}

		public ScheduleHandler<MODE, CONFIG> build() {
			var configs = this.configs.build();
			// Check that all MODEs are properly defined
			// for (var mode : configs.keySet()) {
			// var notDefinedModes = Stream.of(mode.values()) //
			// .filter(m -> !configs.keySet().contains(m)) //
			// .map(m -> m.name()) //
			// .collect(Collectors.joining(", "));
			// if (!notDefinedModes.isEmpty()) {
			// throw new IllegalArgumentException("Modes are not defined: " +
			// notDefinedModes);
			// }
			// }
			return new ScheduleHandler<MODE, CONFIG>(configs);
		}

	}

	/**
	 * Create a {@link ScheduleHandler} builder.
	 *
	 * @param <MODE>   the type of {@link Mode}
	 * @param <CONFIG> the type of Config
	 * @return a {@link Builder}
	 */
	public static <MODE extends Schedule.Mode, CONFIG> Builder<MODE, CONFIG> create() {
		return new Builder<MODE, CONFIG>();
	}

	/**
	 * Creates a {@link ScheduleHandler}.
	 * 
	 * @param <MODE>        the type of {@link Mode}
	 * @param <CONFIG>      the type of Config
	 * @param scheduleModes the array of Schedule-Modes
	 * @return new {@link ScheduleHandler}
	 */
	@SuppressWarnings("unchecked")
	public static <MODE extends Schedule.Mode, CONFIG> ScheduleHandler<MODE, CONFIG> of(
			Schedule.ModeConfig<MODE, CONFIG>[] scheduleModes) {
		ImmutableMap.Builder<MODE, CONFIG> configs = ImmutableMap.builder();
		for (var mode : scheduleModes) {
			configs.put((MODE) mode, mode.getModeConfig());
		}
		return new ScheduleHandler<MODE, CONFIG>(configs.build());
	}

	private final ImmutableMap<MODE, CONFIG> modeConfigs;

	private Schedule<? extends MODE> schedule;

	private ScheduleHandler(ImmutableMap<MODE, CONFIG> modeConfigs) {
		this.modeConfigs = modeConfigs;
	}

	/**
	 * Apply a {@link Schedule}.
	 * 
	 * @param schedule the {@link Schedule}
	 */
	public synchronized void applySchedule(Schedule<? extends MODE> schedule) {
		this.log.info("Apply new Schedule: ");
		this.log.info(schedule.toString());
		this.schedule = schedule;
	}

	/**
	 * Gets the current Mode Config.
	 * 
	 * @return Config
	 */
	public synchronized CONFIG getCurrentModeConfig() {
		if (this.schedule == null) {
			return null;
		}
		var mode = this.schedule.getCurrentMode();
		if (mode == null) {
			return null;
		}
		return this.modeConfigs.get(mode);
	}

	public ImmutableSet<MODE> getAvailableModes() {
		return this.modeConfigs.keySet();
	}

}
