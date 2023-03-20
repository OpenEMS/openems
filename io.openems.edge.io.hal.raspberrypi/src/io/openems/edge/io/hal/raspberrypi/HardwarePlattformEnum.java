package io.openems.edge.io.hal.raspberrypi;

import io.openems.common.types.OptionsEnum;

public enum HardwarePlattformEnum implements OptionsEnum{
	UNDEFINED(-1),
	MODBERRY_X500_CM4(0);

	private int value;
	
	private HardwarePlattformEnum(int value) {
		this.value = value;
	}
	
	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.asCamelCase();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
