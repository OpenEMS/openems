package io.openems.edge.evcs.v2.api;

import io.openems.common.types.OptionsEnum;

public enum ChargingType implements OptionsEnum {

    /**
     * Plug type is unkown.
     */
    UNDEFINED(-1, "Undefined"), //
    /**
     * Plugs using the Combined Charging System standard.
     */
    CCS(0, "CCS"), //
    /**
     * Plugs using the Chademo standard.
     */
    CHADEMO(1, "Chademo"), //
    /**
     * general AC Plugs.
     */
    AC(2, "AC"), //
    /**
     * Plugs using the Tesla Supercharger standard.
     */
    SUPERCHARGER(3, "Supercharger");

    private final int value;
    private final String name;

    private ChargingType(int value, String name) {
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
