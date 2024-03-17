package io.openems.edge.evcs.v2.api;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
    /**
     * state of EVCS is unknown. 
     */
    UNDEFINED(-1, "Undefined"), //
    /**
     * e.g. unplugged, RFID not enabled,...
     */
    NOT_READY_FOR_CHARGING(1, "Not ready for Charging"), //
    /**
     * EMS suspended charging process.
     */
    SUSPENDED(10, "Suspended"),
    /**
     * Waiting for EV charging request.
     */
    READY_FOR_CHARGING(2, "Ready for Charging"), //
    /**
     * EV is charging.
     */
    CHARGING(3, "Charging"), //
    /**
     * EVCS has an internal error.
     */
    ERROR(4, "Error"), //
    /**
     * Charging was rejected by unkown reason.
     */
    CHARGING_REJECTED(5, "Charging rejected"), //
    /**
     * Charging was finished.
     */
    CHARGING_FINISHED(7, "Charging has finished");

    private final int value;
    private final String name;

    private Status(int value, String name) {
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