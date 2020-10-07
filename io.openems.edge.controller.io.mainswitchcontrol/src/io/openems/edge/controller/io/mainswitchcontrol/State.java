package io.openems.edge.controller.io.mainswitchcontrol;

import io.openems.common.types.OptionsEnum;

public enum State  implements OptionsEnum {	

	UNDEFINED(-1, "Undefined"),

	START(0, "Start checking the allowed charge power"),

	CLOSE_CONTACTOR(1, "Send singal to IO - True"),

	OPEN_CONTACTOR(2, "Send Signal to IO - False"),
	
	PEAK_SHAVE(3, "Peak shave ready");

	private final int value;
	private final String name;

	private State(int value, String name) {
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
