package io.openems.edge.energy.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;

public class EnergyScheduleHandler<STATE, CONTEXT> {

	private final Supplier<STATE[]> availableStates;
	private final Supplier<CONTEXT> context;

	private ImmutableMap<ZonedDateTime, Period<STATE>> schedule = ImmutableMap.of();

	public EnergyScheduleHandler(Supplier<STATE[]> availableStates, Supplier<CONTEXT> context) {
		this.availableStates = availableStates;
		this.context = context;
	}

	/**
	 * Gets the available States.
	 * 
	 * @return an Array of States
	 */
	public STATE[] getAvailableStates() {
		return this.availableStates.get();
	}

	/**
	 * Gets the Context.
	 * 
	 * @return the Context
	 */
	public CONTEXT getContext() {
		return this.context.get();
	}

	public static record Period<STATE>(STATE state, Integer essChargeInChargeGrid) {
	}

	/**
	 * Sets the Schedule. Called by Optimizer.
	 * 
	 * @param schedule the Schedule
	 */
	public synchronized void setSchedule(ImmutableMap<ZonedDateTime, Period<STATE>> schedule) {
		this.schedule = schedule;
	}

	/**
	 * Gets the current State or null.
	 * 
	 * @return the State or null
	 */
	public synchronized STATE getCurrentState() {
		return Optional.ofNullable(this.schedule.get(roundDownToQuarter(ZonedDateTime.now()))) //
				.map(Period::state) //
				.orElse(null);
	}

	// TODO hacky... find a better way!
	/**
	 * Gets the current essChargeInChargeGrid or null.
	 * 
	 * @return the essChargeInChargeGrid or null
	 */
	public synchronized Integer getCurrentEssChargeInChargeGrid() {
		return Optional.ofNullable(this.schedule.get(roundDownToQuarter(ZonedDateTime.now()))) //
				.map(Period::essChargeInChargeGrid) //
				.orElse(null);
	}

}
