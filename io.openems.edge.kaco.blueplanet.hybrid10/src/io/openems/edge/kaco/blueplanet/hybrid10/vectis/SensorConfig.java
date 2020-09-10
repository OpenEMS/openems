package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import io.openems.common.types.OptionsEnum;

public enum SensorConfig implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "VECTIS disabled"), //
	INTERNAL(1, "Enabled with internal sensors"), //
	EXTERNAL(2, "Enabled with external sensors"), //
	INTERNAL_AND_EXTERNAL(3, "Enabled with internal and external sensors"); //

	private final int value;
	private final String name;

	private SensorConfig(int value, String name) {
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
		return UNDEFINED;
	}
}