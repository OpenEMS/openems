package io.openems.edge.controller.io.heatingelement.enums;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, "Undefined"),

	/**
	 * The Heating element is inactive.
	 */
	INACTIVE(0, "Inactive"),

	/**
	 * The Heating element is active.
	 */
	ACTIVE(1, "Active"),

	/**
	 * The Heating element is forced to be active.
	 */
	ACTIVE_FORCED(2, "Force active"),

	/**
	 * The Heating element is forced to be active with an energy limit.
	 */
	ACTIVE_FORCED_LIMIT(3, "Force active with limit"),

	/**
	 * The Heating element is done after forced heating.
	 */
	DONE(4, "Done"),

	/**
	 * The heating element can't reach the minimum limit on time.
	 */
	UNREACHABLE(5, "Unreachable"),
	
	/**
	 * The heating element is calibrated to get the power of the levels.
	 */
	CALIBRATION(6, "Calibrating");

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
	public OptionsEnum getUndefined() {
		return Status.UNDEFINED;
	}

	@Override
	public String getName() {
		return this.name;
	}
}