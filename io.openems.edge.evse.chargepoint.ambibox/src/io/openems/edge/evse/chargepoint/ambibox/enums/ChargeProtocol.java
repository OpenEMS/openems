package io.openems.edge.evse.chargepoint.ambibox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Charge Protocol of the charging session.
 */
public enum ChargeProtocol implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	OTHER(0, "Other"), //
	CHADEMO(1, "CHAdeMO"), //
	DIN_70121(2, "DIN 70121"), //
	ISO_15118_2(3, "ISO 15118-2"), //
	ISO_15118_2_VAS(4, "ISO 15118-2 VAS"), //
	ISO_15118_20(5, "ISO 15118-20");

	private final int value;
	private final String name;

	private ChargeProtocol(int value, String name) {
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
