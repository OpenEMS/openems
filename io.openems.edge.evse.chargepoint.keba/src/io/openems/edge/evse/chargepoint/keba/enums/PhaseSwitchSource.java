package io.openems.edge.evse.chargepoint.keba.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseSwitchSource implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(0, "No phase toggle source is available"), //
	VIA_OCPP(1, "Toggle via OCPP"), //
	VIA_REST(2, "Direct toggle command via RESTAPI"), //
	VIA_MODBUS(3, "Toggle via Modbus"), //
	VIA_UDP(4, "Toggle via UDP"); //

	private final int value;
	private final String name;

	private PhaseSwitchSource(int value, String name) {
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
		return NONE;
	}
}