package io.openems.edge.battery.enfasbms.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactorState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	CONTACTOR_STATE_INIT(0, "Contactor init state"), //
	CONTACTOR_STATE_OPEN(1, "Contactor open state"),
	CONTACTOR_STATE_CLOSE_NEGATIVE(2, "Contactor close negative state"),
	CONTACTOR_STATE_PRECHARGING(3, "Contactor precharging state"),
	CONTACTOR_STATE_CLOSE_POSITIVE(4, "Contactor close positive state"),
	CONTACTOR_STATE_CLOSED(5, "Contactor closed state"), //
	CONTACTOR_STATE_OPENING(6, "Contactor opening state"), //
	CONTACTOR_STATE_ERROR(7, "Contactor error state");

	private final int value;
	private final String name;

	private ContactorState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
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
