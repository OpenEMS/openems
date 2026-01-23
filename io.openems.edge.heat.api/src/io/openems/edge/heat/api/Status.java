package io.openems.edge.heat.api;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, "Undefined"),

	/**
	 * The Heatingelement is in standby mode.
	 */
	STANDBY(0, "Standby"),

	/**
	 * The Heatingelement is running using excess energy.
	 */
	EXCESS(1, "Excess"),

	/**
	 * Control is overridden by another system.
	 */
	CONTROL_NOT_ALLOWED(2, "Control is not allowed"),

	/**
	 * The Heatingelement has reached the requested or max temperature.
	 */
	TEMPERATURE_REACHED(3, "Temperature reached"),
	
	/**
	 * No control signal is available.
	 */
	NO_CONTROL_SIGNAL(4, "No control signal"),
	
	/**
	 * An error occurred on the device.
	 */
	ERROR(5, "Error");

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