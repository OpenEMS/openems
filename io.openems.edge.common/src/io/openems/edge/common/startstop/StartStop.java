package io.openems.edge.common.startstop;

import io.openems.common.types.OptionsEnum;

public enum StartStop implements OptionsEnum {
	/**
	 * <ul>
	 * <li>Write: set the Component to an UNDEFINED/initial state
	 * <li>Read: the Component is in an UNDEFINED start/stop state
	 * </ul>
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * <ul>
	 * <li>Write: START the Component.
	 * <li>Read: the Component is STARTED.
	 * </ul>
	 */
	START(1, "Start"),
	/**
	 * <ul>
	 * <li>Write: STOP the Component.
	 * <li>Read: the Component is STOPPED.
	 * </ul>
	 */
	STOP(2, "Stop");

	private int value;
	private String name;

	private StartStop(int value, String name) {
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