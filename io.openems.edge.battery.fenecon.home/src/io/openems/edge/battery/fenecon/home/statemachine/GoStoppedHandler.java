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
	private boolean didProtocolAdd = false;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		final var modbus = battery.getModbus();
		modbus.removeProtocol(battery.id());
		this.didProtocolAdd = false;
		this.timeAtEntry = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsException {
		final var battery = context.getParent();
		var now = Instant.now(context.clock);
		if (Duration.between(this.timeAtEntry, now).getSeconds() > TIMEOUT && !this.didProtocolAdd) {
			this.addAndRetryModbusProtocol(context);
			return State.GO_STOPPED;
		}

		if (battery.getModbusCommunicationFailed()) {
			return State.STOPPED;
		}

		// TODO if battery is not off
		return State.GO_STOPPED;
	}

	private void addAndRetryModbusProtocol(Context context) throws OpenemsException {
		final var battery = context.getParent();
		final var modbus = battery.getModbus();
		modbus.addProtocol(battery.id(), battery.getDefinedModbusProtocol());
		modbus.retryModbusCommunication(battery.id());
		this.didProtocolAdd = true;
	}

}
