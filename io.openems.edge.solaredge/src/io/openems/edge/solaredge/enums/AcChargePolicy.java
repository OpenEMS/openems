package io.openems.edge.solaredge.enums;

import io.openems.common.types.OptionsEnum;

public  enum AcChargePolicy implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	ALWAYS_ALLOWED(1, "Always allowed"), // needed for AC coupling operation. Allows unlimited charging from the AC. When used with Maximize self-consumption, only excess power is used for charging (charging from the grid is not allowed). 
	FIXED_ENERGY_LIMIT(2, "Fixed Energy Limit"), // allows AC charging with a fixed yearly (Jan 1 to Dec 31) limit (needed for meeting ITC regulation in the US)
	PERCENT_OF_PRODUCTION(3, "Percent of Production"); // allows AC charging with a % of system production year to date limit (needed for meeting ITC regulation in the US)

	private final int value;
	private final String name;

	private AcChargePolicy(int value, String name) {
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
