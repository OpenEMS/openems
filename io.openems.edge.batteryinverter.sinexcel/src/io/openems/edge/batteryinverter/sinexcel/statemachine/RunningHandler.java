package io.openems.edge.batteryinverter.sinexcel.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	private final static int POWER_SAVING_MODE_SECONDS = 60;

	private Instant noPowerSince = null;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.noPowerSince = null;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		Sinexcel inverter = context.getParent();

		if (inverter.hasFaults()) {
			return State.UNDEFINED;
		}

		// Mark as started
		inverter._setStartStop(StartStop.START);
		
		// Call the StateMachine here 

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
		Sinexcel inverter = context.getParent();

		if (context.setActivePower != 0 || context.setReactivePower != 0) {
			// Apply power setpoints
			this.noPowerSince = null;

			if (!inverter.getStateOn().orElse(true)) {
				// Start inverter
				context.setInverterOn();
			}

			IntegerWriteChannel setActivePower = inverter.channel(Sinexcel.ChannelId.SET_ACTIVE_POWER);
			setActivePower.setNextWriteValue(context.setActivePower);

			IntegerWriteChannel setReactivePower = inverter.channel(Sinexcel.ChannelId.SET_REACTIVE_POWER);
			setReactivePower.setNextWriteValue(context.setReactivePower);

		} else {
			// Prepare for power-saving mode
			if (this.noPowerSince == null) {
				this.noPowerSince = Instant.now();
			}

			if ( /* time with 0 power passed */
			Duration.between(this.noPowerSince, Instant.now()).getSeconds() > POWER_SAVING_MODE_SECONDS
					/* inverter is running */
					&& inverter.getStateOn().orElse(false)) {
				context.setInverterOff();
			}
		}

	}

}
