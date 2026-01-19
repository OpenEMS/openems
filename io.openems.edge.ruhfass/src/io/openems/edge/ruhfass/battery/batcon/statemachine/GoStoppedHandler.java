package io.openems.edge.ruhfass.battery.batcon.statemachine;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.batcon.enums.ContactorCommand;
import io.openems.edge.ruhfass.battery.batcon.enums.OperationModeBattery;
import io.openems.edge.ruhfass.battery.batcon.statemachine.StateMachine.State;
import io.openems.edge.ruhfass.battery.batcon.utils.Constants;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;
	public final Logger log = LoggerFactory.getLogger(GoStoppedHandler.class);

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		var isMaxAllowedStopTimePassed = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_STOP_TIME;
		battery._setMaxStopTime(isMaxAllowedStopTimePassed);

		if (battery.hasFaults()) {
			return State.ERROR;
		}

		battery.setContactorCommand(ContactorCommand.OPEN);
		final var operationMode = battery.getOperationMode();
		final var kl15State = battery.getKl15State().orElse(1);
		if (operationMode != OperationModeBattery.HV_ACTVE && kl15State == 1) {
			battery.setStatusKl15CanChannel(0);
			return State.GO_STOPPED;
		}
		if (kl15State == 0) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
