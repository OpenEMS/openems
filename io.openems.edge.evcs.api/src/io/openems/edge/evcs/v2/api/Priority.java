package io.openems.edge.evcs.v2.api;

import io.openems.common.types.OptionsEnum;

public enum Priority implements OptionsEnum {
    /**
     * priority is unknown.
     */
    UNDEFINED(-1, "Undefined"), //
    /**
     * EVCS is used for cars with long standing times.
     */
    LOW(1, "Low"), //
    /**
     * EVCS is used for cars with average standing times.
     */
    REGULAR(2, "Regular"), //
    /**
     * EVCS should be charged as fast as possible.
     */
    HIGH(3, "High") //
    ;

    private final int value;
    private final String name;

    private Priority(int value, String name) {
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
