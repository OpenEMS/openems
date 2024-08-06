package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;

/**
 * Managing a heater by an OperationMode has some advantages. Using it the
 * control of the heating device is reduced, but complexity of the controller
 * logic is also reduced! Some heater react immediately (within seconds), some
 * within minutes. The intention is, that if the device is controlled by state,
 * the controller can work by state as well. Thus, the time factor is less
 * critical and thus the controllers intends to be not that complex.
 */
public enum OperationModeRequest implements OptionsEnum {

    /**
     * Unknown state.
     */
    UNDEFINED(-1, "Undefined"), //

    /**
     * The heater is permanently switched off.
     */
    OFF(0, "Off"), //

    /**
     * The heater runs in a less sufficient mode for space heating and hot water
     * production.
     */
    REDUCED(1, "Reduced operation mode"), //

    /**
     * The heater runs in energy-efficient standard operation.
     */
    REGULAR(2, "Default energy-efficient operation"), //

    /**
     * The heater runs in a more sufficient mode for space heating and hot water
     * production.
     */
    INCREASED(3, "Increased operation mode"), //

    /**
     * The heater is permanently switched on.
     */
    ON(4, "On"), //

    /**
     * The heater is controlled by an external device. That means that OpenEMS is
     * taking over full control of the heating device.
     */
    EXTERNAL(5, "External");

    private final int value;
    private final String name;

    OperationModeRequest(int value, String name) {
	this.value = value;
	this.name = name;
    }

    /**
     * Check if the value of the OperationModeRequest matches the given value.
     * 
     * @param operationMode OperationModeRequest as name
     * @return true if OperationModeRequest equals the operationMode.
     */
    public static boolean contains(String operationMode) {
	for (OperationModeRequest omr : OperationModeRequest.values()) {
	    if (omr.name.equals(operationMode)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Check if the value of the OperationModeRequest matches the given value.
     *
     * @param value value of the OperationModeRequest
     * @return true if OperationModeRequest equals a value like given value.
     */
    public static boolean contains(int value) {
	for (OperationModeRequest omr : OperationModeRequest.values()) {
	    if (omr.value == value) {
		return true;
	    }
	}
	return false;
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
