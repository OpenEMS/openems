package io.openems.edge.controller.io.heatingelement.enums;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, "Undefined"),

	/**
	 * The Heatingelement is inactive.
	 */
	INACTIVE(0, "Inactive"),

	/**
	 * The Heatingelement is active.
	 */
	ACTIVE(1, "Active"),

	/**
	 * The Heatingelement is forced to be active.
	 */
	ACTIVE_FORCED(2, "Force active");

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