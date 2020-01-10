package io.openems.edge.controller.timeslotpeakshaving;

import io.openems.common.types.OptionsEnum;

public enum ChargeState implements OptionsEnum {
	/**
	 * Normal charge state: charge till the battery is full
	 */
	NORMAL(0, "Normal charge state, charge until the battery is full"),
	/**
	 * Hysteresis charge state: block charging after 'Normal charge' till the
	 * battery is not anymore completely full
	 */
	HYSTERESIS(1, "Block charging until specified Soc"),
	/**
	 * Force charge state: force full charging just before the high-load timeslot
	 * starts
	 */
	FORCE_CHARGE(2, "Force charge state: force full charging just before the high-load timeslot starts"),
	/**
	 * State where the controller in outside timeslot, and no power contraints are added
	 * 
	 */
	OUTSIDE_TIMESLOT(3, "State where the controller in outside timeslot, and no power contraints are added")
	;
	
	private final int value;
	private final String name;

	private ChargeState(int value, String name) {
		this.value = value;
		this.name = name;
	}
	
	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return NORMAL;
	}
}