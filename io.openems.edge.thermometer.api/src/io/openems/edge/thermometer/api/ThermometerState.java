package io.openems.edge.thermometer.api;

import io.openems.common.types.OptionsEnum;

public enum ThermometerState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    RISING(1, "Rising"),
    FALLING(0, "Falling");

    private final int value;
    private final String name;

    ThermometerState(int value, String name) {
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
