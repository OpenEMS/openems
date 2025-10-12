package io.openems.edge.evcs.hypercharger;

import io.openems.common.types.OptionsEnum;

/**
 * Shows the selected connector of the Hypercharger.
 * 
 * <p>
 * Note: The raw register values are mapped differently depending on firmware
 * version. This enum uses unique internal values and provides mapping methods
 * for different versions.
 * 
 * <p>
 * Firmware version mappings:
 * <ul>
 * <li>Version 1.8 - 2.4: CCS_DC (1), CHAdeMO (2), CCS_AC (3), GBT (4)</li>
 * <li>Version 2.5+: CCS2 (1), CCS1 (2), CHAdeMO (3), CCS_AC (4), GBT (5), MCS
 * (6), NACS (7)</li>
 * </ul>
 */
public enum SelectedConnector implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE_POINT(0, "ChargePoint"), //
	CCS_DC(10, "CCS DC Connector"), // Used for v1.8-2.4
	CCS1(11, "CCS1 Connector"), // Used for v2.5+
	CCS2(12, "CCS2 Connector"), // Used for v2.5+
	CHA_DEMO(20, "CHAdeMO Connector"), //
	CCS_AC(30, "CCS AC Connector"), //
	GBT(40, "GBT Connector"), //
	MCS(60, "MCS Connector"), // v2.5+ only
	NACS(70, "NACS Connector"); // v2.5+ only

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

	/**
	 * Maps raw register value to SelectedConnector based on firmware version.
	 *
	 * @param rawValue           the raw register value
	 * @param isVersion25OrLater true if firmware is v2.5 or later
	 * @return the appropriate SelectedConnector
	 */
	public static SelectedConnector fromRawValue(int rawValue, boolean isVersion25OrLater) {
		if (isVersion25OrLater) {
			// Version 2.5+ mapping
			switch (rawValue) {
			case 0:
				return CHARGE_POINT;
			case 1:
				return CCS2;
			case 2:
				return CCS1;
			case 3:
				return CHA_DEMO;
			case 4:
				return CCS_AC;
			case 5:
				return GBT;
			case 6:
				return MCS;
			case 7:
				return NACS;
			}
		} else {
			// Version 1.8-2.4 mapping
			switch (rawValue) {
			case 0:
				return CHARGE_POINT;
			case 1:
				return CCS_DC;
			case 2:
				return CHA_DEMO;
			case 3:
				return CCS_AC;
			case 4:
				return GBT;
			}
		}
		return UNDEFINED;
	}
}