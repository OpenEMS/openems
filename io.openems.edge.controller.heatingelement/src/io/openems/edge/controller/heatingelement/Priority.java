
package io.openems.edge.controller.heatingelement;

import io.openems.common.types.OptionsEnum;

public enum Priority implements OptionsEnum {
	TIME(0, "TIME"), //
	KILO_WATT_HOUR(1, "Kilo watt hour");//

	private final int value;
	private final String name;

	private Priority(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return TIME;
	}
}