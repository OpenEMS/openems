package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum AlarmCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DATA_0000(0, "Alarm code data 0x0000"),
	DATA_0001(1, "Alarm code data 0x0001"),
	DATA_0002(2, "Alarm code data 0x0002"),
	DATA_0003(3, "Alarm code data 0x0003"),
	DATA_0004(4, "Alarm code data 0x0004");

	private final int value;
	private final String name;

	AlarmCode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
