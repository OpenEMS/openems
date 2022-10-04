package io.openems.edge.controller.timeslotpeakshaving;

import io.openems.common.types.OptionsEnum;

public enum ChargeState implements OptionsEnum {
	/**
	 * Normal charge state: does not charge the battery, it is waiting to go to slow
	 * charge state.
	 */
	NORMAL(0, "Normal charge state, no active power is set"),
	/**
	 * slow charge state: it is slowly charging the battery to make soc 100%, it can
	 * go to either hysteresis state or high threshold phase.
	 */
	SLOWCHARGE(1, "Slowly charging the battery and getting ready for highthreshold timeslot peak shaving"),
	/**
	 * Hysteresis charge state: block charging after 'Normal charge' till the
	 * battery is not anymore completely full.
	 */
	HYSTERESIS(2, "Block charging until specified Soc"),
	/**
	 * high threshold time slot state: in this state the where the peak shaving
	 * happens with the configured peak shave power starts.
	 */
	HIGHTHRESHOLD_TIMESLOT(3,
			"High threshold timeslot: The time range where the peakshaving is performed, this is actually highthreshold period"),;

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
		return NORMAL;
	}
}