package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryStateCommand implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OPEN_CONTACTOR(0, "Open Contactor"), //
	RESERVED_ENABLE_BATTERY(1, "Reserved Enable Battery"), //
	RESERVED_OPERATION(2, "Reserved Operation"), //
	CLOSE_CONTACTOR(4, "Close Contactor"), //
	CLOSE_PRE_CHARGE(8, "Close Pre-Charge"), //
	RES_BMS_STT_CMD_BIT_4(16, "Reserved BMS State Command Bit 4"), //
	RES_BMS_STT_CMD_BIT_5(32, "Reserved BMS State Command Bit 5"), //
	RES_BMS_STT_CMD_BIT_6(64, "Reserved BMS State Command Bit 6"), //
	RES_BMS_STT_CMD_BIT_7(128, "Reserved BMS State Command Bit 7"), //
	RES_BMS_STT_CMD_BIT_8(256, "Reserved BMS State Command Bit 8"), //
	RES_BMS_STT_CMD_BIT_9(512, "Reserved BMS State Command Bit 9"), //
	RES_BMS_STT_CMD_BIT_10(1024, "Reserved BMS State Command Bit 10"), //
	RES_BMS_STT_CMD_BIT_11(2048, "Reserved BMS State Command Bit 11"), //
	RES_BMS_STT_CMD_BIT_12(4096, "Reserved BMS State Command Bit 12"), //
	ReservedD_BMS_SLEEP(8192, "Reservedd BMS-Sleep"), //
	CLEAR_BMS_ERROR(16384, "Clear BMS Error"), //
	RESET_BMS(32768, "Reset BMS") //
	;

	private final int value;
	private final String name;

	private BatteryStateCommand(int value, String name) {
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
