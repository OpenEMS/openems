package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class Running extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		switch (context.component.getCurrentState()) {
		case FAULT:
		case GRID_PRE_CONNECTED:
		case MPPT:
		case NO_ERROR_PENDING:
		case OFF:
		case PRECHARGE:
		case SHUTTING_DOWN:
		case SLEEPING:
		case STANDBY:
		case STARTING:
		case UNDEFINED:
			return State.UNDEFINED;

		case GRID_CONNECTED:
			// All Good

		case THROTTLED:
			// if inverter is throttled, full power is not available, but the device
			// is still working
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);

		// Apply Active and Reactive Power Set-Points
		this.applyPower(context);

		return State.RUNNING;
	}

	/**
	 * Applies the Active and Reactive Power Set-Points.
	 * 
	 * @param context the {@link Context}
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(Context context) throws OpenemsNamedException {
		// TODO apply reactive power
		IntegerWriteChannel wSetPctChannel = context.component
				.getSunSpecChannelOrError(KacoSunSpecModel.S64201.W_SET_PCT);
		IntegerReadChannel maxApparentPowerChannel = context.component
				.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
		int maxApparentPower = maxApparentPowerChannel.value().getOrError();

		// Active Power Set-Point is set in % of maximum active power
		int wSetPct = context.setActivePower * 100 / maxApparentPower;
		wSetPctChannel.setNextWriteValue(wSetPct);
	}

}
