package io.openems.edge.meter.discovergy.jsonrpc;

public enum Field {

	POWER("power"), //
	POWER_L1("power1"), //
	POWER_L2("power2"), //
	POWER_L3("power3"), //
	VOLTAGE_L1("voltage1"), //
	VOLTAGE_L2("voltage2"), //
	VOLTAGE_L3("voltage3"), //
	ENERGY_IN("energy"), //
	ENERGY_OUT("energyOut");

	private final String name;

	private Field(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
