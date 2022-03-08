package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum StatusOfUtilityGrid implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	WAITING_VALID_AC_UTILITY_GRID(1394, "Waiting For Valid AC Utility Grid"), //
	UTILITY_GRID_CONNECTION(1461, "Utility Grid Connection"), //
	WAITING(1466, "Waiting"), //
	INITIALIZATION(1787, "Initialization"), //
	GRID_OPERATION_WITHOUT_FEEDBACK(2183, "Grid Operation Without Feed-Back"), //
	ENERGY_SAVING_IN_UTILITY_GRID(2184, "Energy Saving In The Utility Grid"), //
	END_ENERGY_SAVING_IN_UTILITY_GRID(2185, "End Energy Saving In The Utility Grid"), //
	START_ENERGY_SAVING_IN_UNITILTY_GRID(2186, "Start Energy Saving In The Utility Grid"); //

	private final int value;
	private final String name;

	private StatusOfUtilityGrid(int value, String name) {
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