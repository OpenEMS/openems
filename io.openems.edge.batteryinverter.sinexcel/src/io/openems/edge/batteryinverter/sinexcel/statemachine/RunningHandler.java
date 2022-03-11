package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		if (inverter.hasFaults() || inverter.getBatteryInverterState().get() == Boolean.FALSE) {
			return State.UNDEFINED;
		}

		// Mark as started
		inverter._setStartStop(StartStop.START);

		// Apply Active and Reactive Power Set-Points
		this.applyPower(context);

		inverter.softStart(true);
		inverter.setStartInverter();
		return State.RUNNING;
	}

	/**
	 * Applies the Active and Reactive Power Set-Points.
	 *
	 * @param context the {@link Context}
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		IntegerWriteChannel setActivePower = inverter.channel(Sinexcel.ChannelId.SET_ACTIVE_POWER);
		setActivePower.setNextWriteValue(context.setActivePower);

		IntegerWriteChannel setReactivePower = inverter.channel(Sinexcel.ChannelId.SET_REACTIVE_POWER);
		setReactivePower.setNextWriteValue(context.setReactivePower);
	}
}
