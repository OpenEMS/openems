package io.openems.edge.timedata.api.utils;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.TimedataProvider;

public class CalculateStateCounter {

	private static final Logger LOG = LoggerFactory.getLogger(CalculateStateCounter.class);

	private static enum State {
		TIMEDATA_QUERY_NOT_STARTED, TIMEDATA_QUERY_IS_RUNNING, CALCULATE_STATE_COLLECTOR;
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
	private final TimedataQueryRetryHandler retryHandler;

	/**
	 * Last stored active counter value.
	 */
	private Integer lastStoredCounterValue;

	public CalculateStateCounter(TimedataProvider timedataProvider, ChannelId channelId) {
		this(timedataProvider, channelId, Clock.systemDefaultZone());
	}

	CalculateStateCounter(TimedataProvider timedataProvider, ChannelId channelId, Clock clock) {
		this.timedataProvider = timedataProvider;
		this.channelId = channelId;
		this.retryHandler = new TimedataQueryRetryHandler(LOG, clock, timedataProvider.id(), channelId.id());
	}

	/**
	 * Updates the counter channel by incrementing its value if the condition is
	 * met.
	 * 
	 * <p>
	 * This method manages the lifecycle of the counter:
	 * <ul>
	 * 1. Initializing the value from historical Timedata if not already done.
	 * </ul>
	 * <ul>
	 * 2. Incrementing the counter in the {@link State#CALCULATE_STATE_COLLECTOR}
	 * state.
	 * </ul>
	 * </p>
	 *
	 * @param isActive true if the counter should be incremented in this cycle
	 */
	public void update(boolean isActive) {
		switch (this.state) {
		case TIMEDATA_QUERY_NOT_STARTED -> this.initializeCounterFromTimedata();
		case TIMEDATA_QUERY_IS_RUNNING -> {
			// Wait for result
		}
		case CALCULATE_STATE_COLLECTOR -> {
			if (isActive) {
				this.timedataProvider.channel(this.channelId).setNextValue(++this.lastStoredCounterValue);
			}
		}
		}
	}

	private void initializeCounterFromTimedata() {
		var timedata = this.timedataProvider.getTimedata();
		var componentId = this.timedataProvider.id();
		if (timedata == null || componentId == null) {
			// Wait for Timedata service to appear or Component to be activated
			this.state = State.TIMEDATA_QUERY_NOT_STARTED;

		} else {
			// Do not query Timedata twice
			this.state = State.TIMEDATA_QUERY_IS_RUNNING;

			timedata.getLatestValue(new ChannelAddress(this.timedataProvider.id(), this.channelId.id()))
					.whenComplete((activeTimeOpt, throwable) -> {
						if (throwable != null) {
							if (this.retryHandler.onFailure(throwable) == TimedataQueryRetryHandler.Decision.RETRY) {
								this.state = State.TIMEDATA_QUERY_NOT_STARTED;
								return;
							}

							this.lastStoredCounterValue = 0;
							this.state = State.CALCULATE_STATE_COLLECTOR;
							this.timedataProvider.channel(this.channelId).setNextValue(this.lastStoredCounterValue);
							return;
						}

						this.retryHandler.reset();
						this.state = State.CALCULATE_STATE_COLLECTOR;

						if (activeTimeOpt.isPresent()) {
							try {
								this.lastStoredCounterValue = TypeUtils.getAsType(OpenemsType.INTEGER,
										activeTimeOpt.get());
							} catch (IllegalArgumentException e) {
								this.lastStoredCounterValue = 0;
							}
						} else {
							this.lastStoredCounterValue = 0;
						}
						this.timedataProvider.channel(this.channelId).setNextValue(this.lastStoredCounterValue);
					});
		}
	}

	/**
	 * Resets the counter value to zero.
	 *
	 * <p>
	 * The counter is reset by setting the next value of the associated channel
	 * (identified by {@code channelId}) to 0.
	 */
	public void resetCounter() {
		this.timedataProvider.channel(this.channelId).setNextValue(this.lastStoredCounterValue = 0);
	}

}
