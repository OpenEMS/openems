package io.openems.edge.ess.byd.container;

import io.openems.common.types.OptionsEnum;

public enum SetSystemWorkstate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), 
	STOP(4, "Stop"),
	RUN(64, "Run");
	
	private final int value;
	private final String name;

	private SetSystemWorkstate(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
