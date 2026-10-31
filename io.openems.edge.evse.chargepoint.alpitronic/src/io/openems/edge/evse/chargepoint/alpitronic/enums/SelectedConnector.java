package io.openems.edge.evse.chargepoint.alpitronic.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Shows the selected connector of the Hypercharger.
 * 
 * <p>
 * Raw register mappings differ by firmware version:
 * <ul>
 * <li>v1.8-2.4: raw 0→CHARGE_POINT, 1→CCS_DC, 2→CHAdeMO, 3→CCS_AC, 4→GBT</li>
 * <li>v2.5+: raw 0→CHARGE_POINT, 1→CCS2, 2→CCS1, 3→CHAdeMO, 4→CCS_AC, 5→GBT,
 * 6→MCS, 7→NACS</li>
 * </ul>
 */
public enum SelectedConnector implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE_POINT(0, "ChargePoint"), //
	CCS_DC(10, "CCS_DC Connector (v1.8-v2.4)"), //
	CCS1(11, "CCS1 Connector (v2.5+)"), //
	CCS2(12, "CCS2 Connector (v2.5+)"), //
	CHA_DEMO(20, "CHAdeMO Connector"), //
	CCS_AC(30, "CCS AC Connector"), //
	GBT(40, "GBT Connector"), //
	MCS(60, "MCS Connector (v2.5+)"), //
	NACS(70, "NACS Connector (v2.5+)");

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
	 * Maps raw register value to the appropriate SelectedConnector based on
	 * firmware version.
	 *
	 * @param rawValue           the raw register value
	 * @param isVersion25OrLater true if firmware version is v2.5 or later
	 * @return the matching SelectedConnector, or null if rawValue is null
	 */
	public static SelectedConnector fromRawValue(Integer rawValue, boolean isVersion25OrLater) {
		return rawValue == null ? null : fromRawValue(rawValue.intValue(), isVersion25OrLater);
	}

	/**
	 * Maps raw register value to the appropriate SelectedConnector based on
	 * firmware version.
	 *
	 * @param rawValue           the raw register value
	 * @param isVersion25OrLater true if firmware version is v2.5 or later
	 * @return the matching SelectedConnector, or UNDEFINED if not found
	 */
	public static SelectedConnector fromRawValue(int rawValue, boolean isVersion25OrLater) {
		if (isVersion25OrLater) {
			// v2.5+ mapping
			return switch (rawValue) {
			case 0 -> CHARGE_POINT;
			case 1 -> CCS2;
			case 2 -> CCS1;
			case 3 -> CHA_DEMO;
			case 4 -> CCS_AC;
			case 5 -> GBT;
			case 6 -> MCS;
			case 7 -> NACS;
			default -> UNDEFINED;
			};
		} else {
			// v1.8-v2.4 mapping
			return switch (rawValue) {
			case 0 -> CHARGE_POINT;
			case 1 -> CCS_DC;
			case 2 -> CHA_DEMO;
			case 3 -> CCS_AC;
			case 4 -> GBT;
			default -> UNDEFINED;
			};
		}
	}
}