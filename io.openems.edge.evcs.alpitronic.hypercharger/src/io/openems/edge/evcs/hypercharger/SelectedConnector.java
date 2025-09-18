package io.openems.edge.evcs.hypercharger;

import io.openems.common.types.OptionsEnum;

/**
 * Shows the selected connector of the Hypercharger.
 * 
 * <p>Note: Connector type values changed between firmware versions:
 * <ul>
 * <li>Version 1.8 - 2.4: CCS_DC (1), CHAdeMO (2), CCS_AC (3), GBT (4)</li>
 * <li>Version 2.5+: CCS2 (1), CCS1 (2), CHAdeMO (3), CCS_AC (4), GBT (5), MCS (6), NACS (7)</li>
 * </ul>
 */
public enum SelectedConnector implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE_POINT(0, "ChargePoint"), //
	// For firmware < 2.5
	CCS_DC(1, "CCS_DC Connector"), // Legacy: v1.8-2.4
	// For firmware >= 2.5
	CCS2(1, "CCS2 Connector"), // v2.5+
	CCS1(2, "CCS1 Connector"), // v2.5+
	CHA_DEMO(2, "CHAdeMO Connector"), // v1.8-2.4: value 2, v2.5+: value 3
	CHA_DEMO_V25(3, "CHAdeMO Connector"), // v2.5+ mapping
	CCS_AC(3, "CCS AC Connector"), // v1.8-2.4: value 3, v2.5+: value 4
	CCS_AC_V25(4, "CCS AC Connector"), // v2.5+ mapping
	GBT(4, "GBT Connector"), // v1.8-2.4: value 4, v2.5+: value 5
	GBT_V25(5, "GBT Connector"), // v2.5+ mapping
	MCS(6, "MCS Connector"), // v2.5+ only
	NACS(7, "NACS Connector"); // v2.5+ only

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