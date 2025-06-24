package io.openems.edge.deye.enums;
import io.openems.common.types.OptionsEnum;

public enum LithiumBatteryType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PYLON(0, "Pylon Solax universalCANBus protocol"), //
	RS485_MODBUS(1, "Tianbangda RS485 modbus protocol"), //
	KOK(2, "KOK protocol"),
	KEITH(3, "Keith"),
	TOP_PAY(4, "Top Pay agreement"),
	PAINE(5, "Paine 485 protocol"),
	JELLIS(6, "Jellis 485 protocol"),
	XIN_WANGDA(7, "Xinwangda 485 protocol"),
	XIN_RUINENG(8, "Xin Ruineng 485 protocol"),
	TIANBANGDA(9, "Tianbangda 485 protocol"),
	SHENGGAO(10, "Shenggao Elictriccan protocol")
	;

	private final int value;
	private final String name;

	private LithiumBatteryType(int value, String name) {
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