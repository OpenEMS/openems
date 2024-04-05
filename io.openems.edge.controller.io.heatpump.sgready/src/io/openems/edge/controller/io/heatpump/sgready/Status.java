package io.openems.edge.controller.io.heatpump.sgready;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, false, false, "Undefined"),

	/**
	 * The Lock state is downward compatible with the energy provider block that is
	 * frequently activated at specific times and consists of a maximum 'hard'.
	 */
	LOCK(0, true, false, "Blocks everything till an internal maximum time of default two hours"),

	/**
	 * The heat pump runs in energy-efficient standard operation with proportional
	 * filling of the heat storage tank for the maximum energy provider blocking
	 * period of two hours.
	 */
	REGULAR(1, false, false, "Default energy-efficient operation"),

	/**
	 * The heat pump runs in a more sufficient mode for space heating and hot water
	 * production, to use available surplus power.
	 */
	RECOMMENDATION(2, false, true, "Recommendation to use more available power"),

	/**
	 * The heat pump runs in a definitive start/heat-up mode. Depending on the heat
	 * pump, heating is forced and additional heaters may be switched on.
	 */
	FORCE_ON(3, true, true, "Force all possible consumption of the heat pump");

	private final int value;
	private final String name;
	private final boolean output1;
	private final boolean output2;

	private Status(int value, boolean output1, boolean output2, String name) {
		this.value = value;
		this.name = name;
		this.output1 = output1;
		this.output2 = output2;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Get the value of output 1.
	 *
	 * @return value as boolean
	 */
	public boolean getOutput1() {
		return this.output1;
	}

	/**
	 * Get the value of output 2.
	 *
	 * @return value as boolean
	 */
	public boolean getOutput2() {
		return this.output2;
	}

	@Override
	public OptionsEnum getUndefined() {
		return Status.UNDEFINED;
	}
}