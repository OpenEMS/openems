package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum TemperatureStatus implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    OK(0, "OK"), //
    DISCONNECTED(1, "Disconnected"), //
    SHORT_CIRCUITED(2, "Short circuited"), //
    REVERSE_POLARITY(3, "Reverse polarity"), //
    UNKNOWN(4, "Unknown") //
    ;

    private final int value;
    private final String name;

    private TemperatureStatus(int value, String name) {
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
