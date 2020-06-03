package io.openems.edge.controller.ess.standby.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.StateHandler;

public enum State implements io.openems.edge.common.statemachine.State<State, Context>, OptionsEnum {
	UNDEFINED(-1, new Undefined()), //
	DISCHARGE(1, new Discharge()), //
	SLOW_CHARGE_1(2, new SlowCharge1()), //
	FAST_CHARGE(3, new FastCharge()), //
	SLOW_CHARGE_2(4, new SlowCharge2()), //
	FINISHED(5, new Finished()) //
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
		return this.name();
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
