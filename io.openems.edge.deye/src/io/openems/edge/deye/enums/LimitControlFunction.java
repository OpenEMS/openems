package io.openems.edge.deye.enums;
import io.openems.common.types.OptionsEnum;

public enum LimitControlFunction implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SELLING_ACTIVE(0, "Sell to grid enabled"), //
	BUILT_IN(1, "Built-In enabled"), // Export to Load
	EXTRAPOSITION_ENABLED(2, "Extraposition enabled"); // Export to CT

	private final int value;
	private final String name;

	private LimitControlFunction(int value, String name) {
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