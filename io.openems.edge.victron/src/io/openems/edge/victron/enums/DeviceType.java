package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum DeviceType implements OptionsEnum {
	UNDEFINED("undefined", 0, 0, 0), //
	Multiplus2GX3kVa("Multiplus II 3kVA Single Phase", -2400, 2400, 3000),
	Multiplus2GX5kVa("Multiplus II 5kVA Single Phase", -4000, 4000, 5000),
	Multiplus2GX8kVa("Multiplus II 8kVA Single Phase", -6400, 6400, 8000),
	Multiplus2GX10kVa("Multiplus II 10kVA Single Phase", -8000, 8000, 10000),
	Multiplus2GX15kVa("Multiplus II 15kVA Single Phase", -12000, 12000, 15000),
	Multiplus2GX3kVaL1L2L3("Multiplus II 3kVA Three Phase System", -2400 * 3, 2400 * 3, 3000 * 3),
	Multiplus2GX5kVaL1L2L3("Multiplus II 5kVA Three Phase System", -4000 * 3, 4000 * 3, 5000 * 3),
	Multiplus2GX8kVaL1L2L3("Multiplus II 8kVA Three Phase System", -6400 * 3, 6400 * 3, 8000 * 3),
	Multiplus2GX10kVaL1L2L3("Multiplus II 10kVA Three Phase System", -8000 * 3, 8000 * 3, 10000 * 3),
	Multiplus2GX15kVaL1L2L3("Multiplus II 15kVA Three Phase System", -12000 * 3, 12000 * 3, 15000 * 3);

	private final String displayName;
	private final int acInputLimit;
	private final int acOutputLimit;
	private final int apparentPowerLimit;

	DeviceType(String displayName, int acInputLimit, int acOutputLimit, int apparentPowerLimit) {
		this.displayName = displayName;
		this.acInputLimit = acInputLimit;
		this.acOutputLimit = acOutputLimit;
		this.apparentPowerLimit = apparentPowerLimit;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public int getAcInputLimit() {
		return this.acInputLimit;
	}

	public int getAcOutputLimit() {
		return this.acOutputLimit;
	}

	public int getApparentPowerLimit() {
		return this.apparentPowerLimit;
	}

	@Override
	public int getValue() {
		return ordinal();
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
