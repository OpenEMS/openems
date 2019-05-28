package io.openems.edge.controller.evcs;

import io.openems.common.types.OptionsEnum;

/**
 * The Priorities for charging.
 * Which Component should be preferred
 * 
 * Todo: If more than 2 Apps are installed, the Priority of the Apps should be handled in an extra controller.
 */
public enum Priority implements io.openems.common.types.OptionsEnum{
	
	CAR(0, "Car"),
	STORAGE(1, "Storage");

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
		return CAR;
	}
	
	
	
}
