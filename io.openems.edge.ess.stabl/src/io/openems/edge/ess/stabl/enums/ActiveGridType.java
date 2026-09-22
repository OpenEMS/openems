package io.openems.edge.ess.stabl.enums;

import io.openems.common.types.OptionsEnum;

public enum ActiveGridType implements OptionsEnum {

	// Actvie grid type:
	// 0 = grid-tied, 400V, 50Hz
	// 2 = island mode, 400V, 50Hz

	UNDEFINED(-1, "Undefined"), //
	GRID_TIED(0, "grid-tied, 400V, 50Hz"), //
	ISLAND_MODE(1, "island mode, 400V, 50Hz"), //

	;

	private final int value;
	private final String name;

	private ActiveGridType(int value, String name) {
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
