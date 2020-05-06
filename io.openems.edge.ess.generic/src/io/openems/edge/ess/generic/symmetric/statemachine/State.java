package io.openems.edge.ess.generic.symmetric.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.StateHandler;

public enum State implements io.openems.edge.common.statemachine.State<State, Context>, OptionsEnum {
	UNDEFINED(-1, new Undefined()), //

	START_BATTERY(10, new StartBattery()), //
	START_BATTERY_INVERTER(11, new StartBatteryInverter()), //
	STARTED(12, new Started()), //

	STOP_BATTERY_INVERTER(20, new StopBattery()), //
	STOP_BATTERY(21, new StopBatteryInverter()), //
	STOPPED(22, new Stopped()), //

	ERROR_HANDLING(30, new ErrorHandling()), //
	;

	private final int value;
	protected final StateHandler<State, Context> handler;

	private State(int value, StateHandler<State, Context> handler) {
		this.value = value;
		this.handler = handler;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	@Override
	public StateHandler<State, Context> getHandler() {
		return this.handler;
	}
}
