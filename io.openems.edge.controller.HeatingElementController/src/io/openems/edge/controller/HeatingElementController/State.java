package io.openems.edge.controller.HeatingElementController;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first state.
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * When production is 2000W more than the capacity of the ESS, last phase was undefined.
	 */
	SWITCH_ON_FIRSTPHASE(0, "SWITCH ON first phase"),
	SWITCH_OFF_FIRSTPHASE(1,"SWITCH OFF first phase "),
	SWITCH_ON_SECONDPHASE(0, "SWITCH ON second phase"),
	SWITCH_OFF_SECONDPHASE(1,"SWITCH OFF second phase "),
	SWITCH_ON_THIRDPHASE(0, "SWITCH ON third phase"),
	SWITCH_OFF_THIRDPHASE(1,"SWITCH OFF third phase ");

	private final int value;
	private final String name;

	private State(int value, String name) {
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