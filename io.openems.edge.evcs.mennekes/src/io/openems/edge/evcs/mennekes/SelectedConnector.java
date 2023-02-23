package io.openems.edge.evcs.mennekes;

import io.openems.common.types.OptionsEnum;

/**
 * Shows the selected connector of the Mennekes charger.
 */
public enum SelectedConnector implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE_POINT(0, "ChargePoint"), //
	CCS_DC(1, "CCS_DC Connector"), //
	CHA_DEMO(2, "CHAdeMO Connector"), //
	CCS_AC(3, "CCS AC Connector");

	private final int value;
	private final String name;

	private SelectedConnector(int value, String name) {
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