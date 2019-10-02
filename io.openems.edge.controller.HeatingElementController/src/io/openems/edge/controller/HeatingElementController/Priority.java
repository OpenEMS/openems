
package io.openems.edge.controller.HeatingElementController;

import io.openems.common.types.OptionsEnum;

public enum Priority implements OptionsEnum{
	

	TIME(0, "Car"),
	KILO_WATT_HOUR(1, "Storage");

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