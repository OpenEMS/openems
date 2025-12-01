package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

/*
 *
 * ATTENTION: Differs from ChargeStateEss
 *
 * */
public enum ChargeState implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    OFF(0, "Off"), //
    FAULT(2, "Fault"), //
    BULK(3, "Bulk"), //
    ABSORPTION(4, "Absorption"), //
    FLOAT(5, "Float"), //
    STORAGE(6, "Storage"), //
    EQUALIZE(7, "Equalize"), //
    OTHER(11, "Other (Hub-1)"), //
    EXTERNAL_CONTROL(252, "External control") //
    ;

    private final int value;
    private final String name;

    private ChargeState(int value, String name) {
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
