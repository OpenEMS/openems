package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum TemperatureType implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    BATTERY(0, "Battery"), //
    FRIDGE(1, "Fridge"), //
    GENERIC(2, "Generic") //
    ;

    private final int value;
    private final String name;

    private TemperatureType(int value, String name) {
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
