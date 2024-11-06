package io.openems.edge.io.revpi.bsp.core;

import io.openems.common.types.OptionsEnum;

public enum LedState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    OFF(0, "Off"), //
    RED(1, "Red"), //
    GREEN(2, "Green"), //
    ORANGE(3, "Orange"), //
    RED_BLINK(4, "RedBlink"), //
    GREEN_BLINK(5, "GreenBlink"), //
    ORANGE_BLINK(6, "OrangeBlink"), //
    ;

    private final int value;
    private final String name;

    private LedState(int value, String name) {
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
