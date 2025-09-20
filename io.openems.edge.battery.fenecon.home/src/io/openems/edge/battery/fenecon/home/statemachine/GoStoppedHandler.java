package io.openems.edge.battery.fenecon.home.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private static int TIMEOUT = 2100; // [35 minutes in seconds]
	private Instant timeAtEntry = Instant.MIN;
	private boolean isProtocolAdded = false;

	@Override
	protected void onEntry(Context context) {
		// Remove the protocol to trigger BMS timeout
		this.removeProtocol(context);

		this.timeAtEntry = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsException {
		var now = Instant.now(context.clock);
		if (Duration.between(this.timeAtEntry, now).getSeconds() > TIMEOUT && !this.isProtocolAdded) {
			this.addProtocol(context);
			return State.GO_STOPPED;
		}

		final var battery = context.getParent();
		if (battery.getModbusCommunicationFailed()) {
			return State.STOPPED;
		}

		// TODO if battery is not off
		return State.GO_STOPPED;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		// Make sure to leave this GoStoppedHandler with added protocol
		if (!this.isProtocolAdded) {
			this.addProtocol(context);
		}
	}

	private void addProtocol(Context context) {
		final var battery = context.getParent();
		final var modbus = battery.getModbus();
		this.isProtocolAdded = true;
		modbus.addProtocol(battery.id(), battery.getDefinedModbusProtocol());
	}

	private void removeProtocol(Context context) {
		final var battery = context.getParent();
		final var modbus = battery.getModbus();
		this.isProtocolAdded = false;
		modbus.removeProtocol(battery.id());
	}
}
