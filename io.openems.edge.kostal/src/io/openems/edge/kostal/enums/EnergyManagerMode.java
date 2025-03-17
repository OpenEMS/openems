package io.openems.edge.kostal.enums;

import io.openems.common.types.OptionsEnum;

// 0x68 104 State of energy manager - U32 2 RO 0x03
public enum EnergyManagerMode implements OptionsEnum {
    IDLE(0x00, "Idle"), //
    NOT_AVAILABLE_1(0x01, "n/a"), //
    EMERGENCY_BATTERY_CHARGE(0x02, "Emergency Battery Charge"), //
    NOT_AVAILABLE_2(0x04, "n/a"), //
    WINTER_MODE_STEP_1(0x08, "Winter Mode Step 1"), //
    WINTER_MODE_STEP_2(0x10, "Winter Mode Step 2"), //
    UNDEFINED(-1, "Undefined");

    private final int value;
    private final String name;

    private EnergyManagerMode(int value, String name) {
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
