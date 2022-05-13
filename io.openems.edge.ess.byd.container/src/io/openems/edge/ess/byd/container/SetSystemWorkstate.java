package io.openems.edge.ess.byd.container;

import io.openems.common.types.OptionsEnum;

public enum SetSystemWorkstate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(2, "Stop"), //
	RUN(6, "Run");

	private final int value;
	private final String name;

	private SetSystemWorkstate(int value, String name) {
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
