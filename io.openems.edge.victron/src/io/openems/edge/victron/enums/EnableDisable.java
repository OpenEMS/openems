package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum EnableDisable implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    DISABLE(0, "Disable"), //
    ENABLE(1, "Enable") //
    ;

    private final int value;
    private final String name;

    private EnableDisable(int value, String name) {
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