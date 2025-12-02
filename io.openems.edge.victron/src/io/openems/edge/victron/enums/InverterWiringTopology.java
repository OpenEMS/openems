package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum InverterWiringTopology implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    THREE_PHASE_FOUR_WIRE(0, "3P4W"), //
    THREE_PHASE_THREE_WIRE(1, "3P3W or 3P3W+N") //
    ;

    private final int value;
    private final String name;

    private InverterWiringTopology(int value, String name) {
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