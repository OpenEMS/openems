package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum OutputFrequencyLevel implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    Hz_50(0, "50 Hz"), //
    Hz_60(1, "60 Hz") //
    ;

    private final int value;
    private final String name;

    private OutputFrequencyLevel(int value, String name) {
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