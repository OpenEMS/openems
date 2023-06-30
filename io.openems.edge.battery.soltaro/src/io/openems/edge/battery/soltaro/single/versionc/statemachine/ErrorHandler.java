package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	// private void handleErrorsWithReset() {
	// // To reset , first sleep and then reset the system
	// switch (this.resetState) {
	// case NONE:
	// this.resetState = ResetState.SLEEP;
	// break;
	// case SLEEP:
	// this.sleepSystem();
	// this.resetState = ResetState.RESET;
	// break;
	// case RESET:
	// this.resetSystem();
	// this.resetState = ResetState.FINISHED;
	// break;
	// case FINISHED:
	// this.resetState = ResetState.NONE;
	// this.setStateMachineState(State.ERRORDELAY);
	// resetDone = true;
	// break;
	// }
	// }

	// private void resetSystem() {
	// EnumWriteChannel resetChannel =
	// this.channel(SingleRackVersionC.ChannelId.SYSTEM_RESET);
	// try {
	// resetChannel.setNextWriteValue(SystemReset.ACTIVATE);
	// } catch (OpenemsNamedException e) {
	// // TODO should throw an exception
	// System.out.println("Error while trying to reset the system!");
	// }
	// }

	// private void sleepSystem() {
	// EnumWriteChannel sleepChannel =
	// this.channel(SingleRackVersionC.ChannelId.SLEEP);
	// try {
	// sleepChannel.setNextWriteValue(Sleep.ACTIVATE);
	// } catch (OpenemsNamedException e) {
	// // TODO should throw an exception
	// System.out.println("Error while trying to send the system to sleep!");
	// }
	// }

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Try to stop system
		var battery = context.getParent();
		battery.setPreChargeControl(PreChargeControl.SWITCH_OFF);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery._setMaxStartAttempts(false);
		battery._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
