package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum ManualStart implements OptionsEnum {

    UNDEFINED(-1, "Undefined"), //
    STOP_GENERATOR(0, "Stop generator"), //
    START_GENERATOR(1, "Start generator") //
    ;

    private final int value;
    private final String name;

    private ManualStart(int value, String name) {
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
