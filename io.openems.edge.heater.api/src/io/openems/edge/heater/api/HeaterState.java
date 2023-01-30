package io.openems.edge.heater.api;

import java.util.Arrays;

import io.openems.common.types.OptionsEnum;

/**
 * The possible states of a heater.
 */

public enum HeaterState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    BLOCKED_OR_ERROR(0, "Heater operation is blocked by something"), //
    OFF(1, "Off"), //
    STANDBY(2, "Standby, waiting for commands"), //
    STARTING_UP_OR_PREHEAT(3, "Command to heat received, preparing to start heating"), //
    RUNNING(4, "Heater is running") //
    ;

    private final int value;
    private final String name;

    HeaterState(int value, String name) {
	this.value = value;
	this.name = name;
    }

    /**
     * Check if the name of the HeaterState matches the given String.
     *
     * @param state name of the HeaterState
     * @return true if HeaterState contains a name like given state.
     */
    public static boolean contains(String state) {
	return Arrays.stream(HeaterState.values()).anyMatch(entry -> entry.name.equals(state));
    }

    /**
     * Check if the value of the HeaterState matches the given value.
     *
     * @param stateValue value of the HeaterState
     * @return true if HeaterState contains a name like given state.
     */
    public static boolean contains(int stateValue) {
	return Arrays.stream(HeaterState.values()).anyMatch(entry -> entry.value == stateValue);
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

    /**
     * Returns the enum state corresponding to the integer value.
     *
     * @param value the integer value of the enum
     * @return the enum state
     */
    public static HeaterState valueOf(int value) {
	HeaterState returnEnum = HeaterState.UNDEFINED;
	switch (value) {
	case 0:
	    returnEnum = HeaterState.BLOCKED_OR_ERROR;
	    break;
	case 1:
	    returnEnum = HeaterState.OFF;
	    break;
	case 2:
	    returnEnum = HeaterState.STANDBY;
	    break;
	case 3:
	    returnEnum = HeaterState.STARTING_UP_OR_PREHEAT;
	    break;
	case 4:
	    returnEnum = HeaterState.RUNNING;
	    break;
	}
	return returnEnum;
    }
}