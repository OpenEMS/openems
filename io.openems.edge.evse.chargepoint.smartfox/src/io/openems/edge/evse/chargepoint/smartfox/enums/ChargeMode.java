package io.openems.edge.evse.chargepoint.smartfox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Charge mode of a SMARTFOX Car Charger, as reported via the Modbus register
 * "car charge n charge mode" (0 = surplus, 1 = manual mode).
 */
public enum ChargeMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SURPLUS(0, "Surplus"), //
	MANUAL(1, "Manual");

	private final int value;
	private final String name;

	private ChargeMode(int value, String name) {
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
