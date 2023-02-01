package io.openems.edge.timedata.api.utils;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.TimedataProvider;

public class CalculateActiveTime {

	/**
	 * Available States.
	 *
	 * <p>
	 * IMPLEMENTATION NOTE: we are using a custom StateMachine here and not the
	 * generic implementation in 'io.openems.edge.common.statemachine', because one
	 * State-Machine per EnergyCalculator object is required, which is not possible
	 * in the generic static enum implementation.
	 */
	private static enum State {
		TIMEDATA_QUERY_NOT_STARTED, TIMEDATA_QUERY_IS_RUNNING, CALCULATE_TIME_OPERATION;
	}

	/**
	 * Keeps the current State.
	 */
	private State state = State.TIMEDATA_QUERY_NOT_STARTED;

	/**
	 * Keeps the target {@link ChannelId} of the time channel.
	 */
	private final ChannelId channelId;

	private final TimedataProvider timedataProvider;

	/**
	 * Keeps the time stamp of the last data.
	 */
	private Instant lastTimestamp = null;

	/**
	 * Keeps the given isActive value.
	 */
	private Boolean lastIsActive = null;

	/**
	 * Last stored active time.
	 */
	private Long lastStoredActiveTime;

	/**
	 * ContinuousCumulatedEnergy keeps the exceeding time in [msec]. It is
	 * continuously updated during CALCULATE_ENERGY_OPERATION state.
	 */
	private long continuousTime = 0L;

	public CalculateActiveTime(TimedataProvider timedataProvider, ChannelId channelId) {
		this.timedataProvider = timedataProvider;
		this.channelId = channelId;
	}

	/**
	 * Counts up the time channel if the given value is true.
	 *
	 * @param isActive boolean if the corresponding channel should be updated.
	 */
	public void update(boolean isActive) {

		switch (this.state) {
		case TIMEDATA_QUERY_NOT_STARTED:
			this.initializeActiveTimeFromTimedata();
			break;

		case TIMEDATA_QUERY_IS_RUNNING:
			// Wait for result
			break;

		case CALCULATE_TIME_OPERATION:
			this.calculateActiveTime(isActive);
			break;
		}

		// Keep last data for next run
		this.lastTimestamp = Instant.now();
		this.lastIsActive = isActive;
	}

	private void initializeActiveTimeFromTimedata() {
		var timedata = this.timedataProvider.getTimedata();
		var componentId = this.timedataProvider.id();
		if (timedata == null || componentId == null) {
			// Wait for Timedata service to appear or Component to be activated
			this.state = State.TIMEDATA_QUERY_NOT_STARTED;

		} else {
			// Do not query Timedata twice
			this.state = State.TIMEDATA_QUERY_IS_RUNNING;

			timedata.getLatestValue(new ChannelAddress(this.timedataProvider.id(), this.channelId.id()))
					.thenAccept(activeTimeOpt -> {
						this.state = State.CALCULATE_TIME_OPERATION;

						if (activeTimeOpt.isPresent()) {
							try {
								this.lastStoredActiveTime = TypeUtils.getAsType(OpenemsType.LONG, activeTimeOpt.get());
							} catch (IllegalArgumentException e) {
								this.lastStoredActiveTime = 0L;
							}
						} else {
							this.lastStoredActiveTime = 0L;
						}
					});
		}
	}

	/**
	 * Calculate the active time.
	 *
	 * @param isActive currently active?
	 */
	private void calculateActiveTime(boolean isActive) {
		if (this.lastTimestamp != null && this.lastStoredActiveTime != null && this.lastIsActive && isActive) {

			// Calculate duration since last value
			var duration /* [msec] */ = Duration.between(this.lastTimestamp, Instant.now()).toMillis();

			// Add to continuous cumulated time
			this.continuousTime += duration;

			// Update last active time if 1 second passed
			if (this.continuousTime > 1_000 /* 1 sec */) {
				this.lastStoredActiveTime += this.continuousTime / 1_000;
				this.continuousTime %= 1000;
			}
		}

		// Update 'cumulated time'
		this.timedataProvider.channel(this.channelId).setNextValue(this.lastStoredActiveTime);
	}
}
