package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterLocationCode implements OptionsEnum {

    UNDEFINED(-1, "Undefined"),

    GRID(1, "Grid"),
    LOAD(2, "Load"),
    GRID_PV_TWO_METER(3, "Grid + PV (Two Meter)");

    private final int value;
    private final String name;

    MeterLocationCode(int value, String name) {
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

    public static MeterLocationCode fromValue(int value) {
        for (var v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return UNDEFINED;
    }
}
