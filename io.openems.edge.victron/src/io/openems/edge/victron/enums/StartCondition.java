package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum StartCondition implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    STOPPED(0, "Stopped"), //
    MANUAL(1, "Manual"), //
    TEST_RUN(2, "TestRun"), //
    LOSS_OF_COMMS(3, "LossOfComms"), //
    SOC(4, "Soc"), //
    AC_LOAD(5, "AcLoad"), //
    BATTERY_CURRENT(6, "BatteryCurrent"), //
    BATTERY_VOLTAGE(7, "BatteryVoltage"), //
    INVERTER_TEMPERATURE(8, "InverterTemperatur"), //
    INVERTER_OVERLOAD(9, "InverterOverload"), //
    STOP_ON_AC_1(10, "StopOnAc1") //
    ;

    private final int value;
    private final String name;

    private StartCondition(int value, String name) {
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
