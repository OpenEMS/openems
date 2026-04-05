package io.openems.edge.ruhfass.battery.batcon.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.batcon.enums.ContactorCommand;
import io.openems.edge.ruhfass.battery.batcon.enums.OperationModeBattery;
import io.openems.edge.ruhfass.battery.batcon.statemachine.StateMachine.State;
import io.openems.edge.ruhfass.battery.batcon.utils.Constants;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		this.timeAtEntry = Instant.now();
		battery._setMaxStartTime(false);

	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		boolean isMaxAllowedStartTimePassed = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_START_TIME;
		battery._setMaxStartTime(isMaxAllowedStartTimePassed);

		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}
		battery.setStatusKl15CanChannel(1);
		final var kl15State = battery.getKl15State().orElse(0);
		if (battery.getOperationMode() == OperationModeBattery.HV_INACTVE && kl15State == 1) {
			battery.setContactorCommand(ContactorCommand.CLOSE);
			return State.GO_RUNNING;
		}

		if (battery.getOperationMode() == OperationModeBattery.HV_ACTVE) {
			return State.RUNNING;
		}

		return State.GO_RUNNING;
	}
}
