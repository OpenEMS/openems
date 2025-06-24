package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;

public enum EnergyManagementModel implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    BATTERY_FIRST(0, "Battery priority. Load battery before balancing grid connection"),
    LOAD_FIRST(1, "Load priority.");   

	private final int value;
	private final String name;

	private EnergyManagementModel(int value, String name) {
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


