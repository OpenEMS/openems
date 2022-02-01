package io.openems.edge.battery.soltaro.single.versionc.enums;

import io.openems.common.types.OptionsEnum;

/**
 * NOTE.
 *
 * <ul>
 * <li>Standby <-> Charge&Discharge forbidden but main contactor keeps on
 * <li>Stop <-> Charge&Discharge forbidden while main contactor keeps off
 * </ul>
 */
public enum ClusterRunState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	FULL(0x1, "Full"), //
	EMPTY(0x2, "Empty"), //
	STANDBY(0x3, "Standby"), //
	STOP(0x4, "Stop");

	private int value;
	private String name;

	private ClusterRunState(int value, String name) {
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