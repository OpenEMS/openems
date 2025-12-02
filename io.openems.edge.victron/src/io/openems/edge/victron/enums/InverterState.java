package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum InverterState implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    OFF(0, "Off"), //
    LOW_POWER_MODE(2, "Low power mode (search mode)"), //
    FAULT(2, "Fault"), //
    INVERTING(9, "Inverting (on)") //
    ;

    private final int value;
    private final String name;

    private InverterState(int value, String name) {
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
