package io.openems.edge.evse.chargepoint.ambibox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Control Mode of the charger.
 */
public enum ControlMode implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	CONTROLLABLE(1, "Controllable"), //
	LIMITABLE(2, "Limitable");

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
