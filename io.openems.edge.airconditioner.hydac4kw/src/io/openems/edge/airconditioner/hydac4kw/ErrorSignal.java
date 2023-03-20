package io.openems.edge.airconditioner.hydac4kw;

import io.openems.common.types.OptionsEnum;

public enum ErrorSignal implements OptionsEnum {
	STATE_UNDEFINED(0),
	STATE_NORMAL(1),
	STATE_ALARM(2);
	
	int value;
	
	ErrorSignal(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.toString();
	}

	@Override
	public OptionsEnum getUndefined() {
		return STATE_UNDEFINED;
	}

}
