package io.openems.edge.common.offgrid;

import io.openems.common.types.OptionsEnum;

public enum GridType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	THREE_PHASE_FOUR_WIRE(0, "3 Phase 4 Wire"), //
	THREE_PHASE_THREE_WIRE(1, "3 Phase 3 Wire"); //

	private int value;
	private String name;

	private GridType(int value, String name) {
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