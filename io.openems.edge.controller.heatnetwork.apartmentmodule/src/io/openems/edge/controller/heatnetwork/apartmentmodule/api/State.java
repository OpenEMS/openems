package io.openems.edge.controller.heatnetwork.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    IDLE(0, "Idle"),
    HEAT_PUMP_ACTIVE(1, "HeatPumpActive"),
    EXTRA_HEAT(2, "ExtraHeat"),
    EMERGENCY_ON(20, "EmergencyOn"),
    EMERGENCY_STOP(30, "EmergencyOff");

    private int value;
    private String name;


    State(int value, String name) {
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
