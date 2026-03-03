package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterTypeCode implements OptionsEnum {

    UNDEFINED(-1, "Undefined"),

    GENERAL_1PH(1, "General 1-Phase"),
    ACREL_3PH(2, "Acrel 3-Phase"),
    GENERAL_3PH(3, "General 3-Phase"),
    EASTRON_1PH(4, "Standard Eastron 1-Phase"),
    EASTRON_3PH(5, "Standard Eastron 3-Phase"),
    NO_METER_MODE(6, "No Meter Mode"),
    CHINT_SPLIT_PHASE(7, "Chint Split Phase Meter");

    private final int value;
    private final String name;

    MeterTypeCode(int value, String name) {
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

    public static MeterTypeCode fromValue(int value) {
        for (var v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        return UNDEFINED;
    }
}
