package io.openems.edge.evse.chargepoint.mennekes.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseSwitchMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	USE_ONE_PHASE(0, "Use 1 phase only"), //
	USE_THREE_PHASE(1, "Use 3 phases only"), //
	DYNAMIC_PHASE_SWITCH(2, "Use dynamic phase switch"), //
	PHASE_SWITCH_NO_EV(3, "Phase switch only if no EV is connected"), //
	;

	private final int value;
	private final String name;

	private PhaseSwitchMode(int value, String name) {
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