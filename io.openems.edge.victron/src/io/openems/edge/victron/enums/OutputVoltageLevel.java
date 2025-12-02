package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum OutputVoltageLevel implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    V_380(0, " 380 V"), //
    V_400(1, "400 V"), //
    V_480(2, "480 V") //
    ;

    private final int value;
    private final String name;

    private OutputVoltageLevel(int value, String name) {
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