package io.openems.edge.evcs.evtec;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.TimedataProvider;

public class AccumulateEnergy {

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
		TIMEDATA_QUERY_NOT_STARTED, TIMEDATA_QUERY_IS_RUNNING, CALCULATE_ENERGY_OPERATION;
	}

	/**
	 * Keeps the current State.
	 */
	private State state = State.TIMEDATA_QUERY_NOT_STARTED;

	private final TimedataProvider component;

	/**
	 * Keeps the target {@link ChannelId} of the Energy channel.
	 */
	private final ChannelId channelId;

	/**
	 * energyBeforeSession keeps the energy before the session start in [Wh]. It is
	 * initialized during TIMEDATA_QUERY_* states.
	 */
	private Long energyBeforeSession = null;

	private Long energyTotal = null;

	public AccumulateEnergy(TimedataProvider component, ChannelId channelId) {
		this.component = component;
		this.channelId = channelId;
	}

	/**
	 * Update the Channel.
	 *
	 * @param energySession the latest energy session value in [Wh]
	 */
	public void update(Integer energySession) {
		switch (this.state) {
		case TIMEDATA_QUERY_NOT_STARTED:
			this.initializeEnergyTotalFromTimedata();
			break;

		case TIMEDATA_QUERY_IS_RUNNING:
			// wait for result
			break;

		case CALCULATE_ENERGY_OPERATION:
			this.accumulateEnergy(energySession);
			break;
		}

	}

	/**
	 * Initialize energy total value from from Timedata service.
	 */
	private void initializeEnergyTotalFromTimedata() {
		var timedata = this.component.getTimedata();
		var componentId = this.component.id();
		if (timedata == null || componentId == null) {
			// Wait for Timedata service to appear or Component to be activated
			this.state = State.TIMEDATA_QUERY_NOT_STARTED;

		} else {
			// do not query Timedata twice
			this.state = State.TIMEDATA_QUERY_IS_RUNNING;

			timedata.getLatestValue(new ChannelAddress(this.component.id(), this.channelId.id()))
					.thenAccept(energyTotalOpt -> {
						this.state = State.CALCULATE_ENERGY_OPERATION;

						if (energyTotalOpt.isPresent()) {
							try {
								this.energyTotal = TypeUtils.getAsType(OpenemsType.LONG, energyTotalOpt.get());
							} catch (IllegalArgumentException e) {
								this.energyTotal = 0L;
							}
						} else {
							this.energyTotal = 0L;
						}
					});
		}
	}

	/**
	 * Calculate the accumulated energy.
	 *
	 * @param energySession the session energy
	 */
	private void accumulateEnergy(Integer energySession) {
		if (energySession == 0) {
			this.energyBeforeSession = this.energyTotal;
		}

		this.energyTotal = this.energyBeforeSession + energySession;
		this.component.channel(this.channelId).setNextValue(this.energyTotal);
	}
}