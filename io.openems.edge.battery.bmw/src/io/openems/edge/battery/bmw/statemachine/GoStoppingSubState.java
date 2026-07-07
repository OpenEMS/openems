package io.openems.edge.battery.bmw.statemachine;

import io.openems.common.types.OptionsEnum;

public enum GoStoppingSubState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	STOP_BATTERY(0, "Stop battery"), //
	DEACTIVATE_INVERTER_RELEASE(1, "Deactivate inverter release"), //
	CHECK_INVERTER_RELEASE_OFF(2, " Check inverter release off"), //
	DEACTIVATE_BCS_POWER_STATE(3, "Deactivate bcs power state"), //
	CHECK_BCS_POWER_STATE_OFF(4, "Check bcs power state off"), //
	FINISHED(5, "finished"), //
	ERROR(6, " Error ");

	private final int value;
	private final String name;

	private GoStoppingSubState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
