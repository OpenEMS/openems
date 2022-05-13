package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum GeneratorStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	ERROR(1392, "Error"), //
	INITIALIZATION(1787, "Initialization"), //
	READY(1788, "Ready"), //
	WARM_UP(1789, "Warm-Up"), //
	SYNCHRONIZE(1790, "Synchronize"), //
	ACTIVATED(1791, "Activated"), //
	RE_SYNCHRONIZE(1792, "Re-Synchronize"), //
	GENERATOR_SEPERATION(1793, "Generator Separation"), //
	SHUTOFF_DELAY(1794, "Shut-Off Delay"), //
	BLOCKED(1795, "Blocked"), //
	BLOCKED_AFTER_ERROR(1796, "Blocked After Error"); //

	private final int value;
	private final String name;

	private GeneratorStatus(int value, String name) {
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