package io.openems.edge.evcc.loadpoint;

import io.openems.common.types.OptionsEnum;

/**
 * Generic plug/cable state for EVCC loadpoints.
 *
 * <p>
 * Provides a simple, vendor-agnostic representation of charger connection
 * state. EVCC only reports a boolean "connected" status, so this enum
 * represents just the two states that EVCC can distinguish.
 */
public enum PlugState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNPLUGGED(0, "Unplugged"), //
	CONNECTED(7, "Connected"); //

	private final int value;
	private final String name;

	private PlugState(int value, String name) {
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
