package io.openems.edge.evcs.cluster;

import io.openems.common.types.OptionsEnum;

public enum EvcsClusterStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	REGULAR(0, "Regular state"), //
	DECREASING(1, "Reducing the charging limit/limits and waiting for evcs-specific reaction time"), //
	INCREASING(2, "Increasing the charging limit/limits and waiting for evcs-specific reactiontime");

	private final int value;
	private final String name;

	private EvcsClusterStatus(int value, String name) {
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