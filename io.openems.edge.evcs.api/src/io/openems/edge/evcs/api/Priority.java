package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum Priority implements OptionsEnum {
    /**
     * priority is unknown.
     */
    UNDEFINED(-1, "Undefined"), //
    /**
     * EVCS should only be charged at very low priority, e.g only if there is excess
     * power.
     */
    VERY_LOW(0, "Very low"),
    /**
     * It is not necessary to charge the EVCS fastly, e.g. a car will be attached
     * for a long time or is almost full.
     */
    LOW(1, "Low"), //
    /**
     * EVCS should be charged in medium time.
     */
    MEDIUM(2, "Medium"), //
    /**
     * EVCS should be charged fastly.
     */
    HIGH(3, "High"), //
    /**
     * EVCS should be charged as fast as possible.
     */
    VERY_HIGH(4, "Very high");

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
