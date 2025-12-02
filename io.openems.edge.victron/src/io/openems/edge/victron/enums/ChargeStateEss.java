package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Attention!. Differs from ChargeState (refers to batteryinverter).
 */
public enum ChargeStateEss implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	INITIALISING(0, "Initialising"), //
	BULK(1, "Bulk"), //
	ABSORPTION(2, "Absorption"), //
	FLOAT(3, "Float"), //
	STORAGE(4, "Storage"), //
	ABSORPTION_REPEAT(5, "Absorption Repeat"), //
	ABSORPTION_FORCED(6, "Absorption Forced"), //
	EQUALIZE(7, "Equalize"), //
	BULK_STOPPED(1, "Bulk Stopped"), //
	UNKNOWN(9, "Unknown");

	private final int value;
	private final String name;

	private ChargeStateEss(int value, String name) {
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
