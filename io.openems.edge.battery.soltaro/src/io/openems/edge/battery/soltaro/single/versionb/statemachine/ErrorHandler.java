package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
		context.component.stopSystem();
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.component._setMaxStartAttempts(false);
		context.component._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		System.out.println("Stuck in ERROR_HANDLING: " + context.component.getStateChannel().listStates());

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
