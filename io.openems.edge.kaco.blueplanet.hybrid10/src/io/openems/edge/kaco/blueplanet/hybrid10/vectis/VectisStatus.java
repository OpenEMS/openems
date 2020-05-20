package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import io.openems.common.types.OptionsEnum;

public enum VectisStatus implements OptionsEnum {
    UNDEFINED(-2, "Undefined"), //
    NOT_CONNECTED(-1, "Unknown (VECTIS not connected)"), //
    ON_GRID(0, "On-Grid mode"), //
    OFF_GRID(1, "Off-Grid mode"); //

    private final int value;
    private final String name;

    private VectisStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return UNDEFINED;
    }
}