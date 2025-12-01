package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum EqualizationPending implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    NO(0, "No"), //
    YES(1, "Yes"), //
    ERROR(2, "Error"), //
    UNAVAILABLE(3, "Unavailable - Unknown") //
    ;

    private final int value;
    private final String name;

    private EqualizationPending(int value, String name) {
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
