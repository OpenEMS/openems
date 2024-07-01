package io.openems.edge.meter.discovergy.jsonrpc;

public enum Field {

	POWER("power"), //
	POWER1("power1"), //
	POWER2("power2"), //
	POWER3("power3"), //
	VOLTAGE1("voltage1"), //
	VOLTAGE2("voltage2"), //
	VOLTAGE3("voltage3"), //
	ENERGY("energy"), //
	ENERGY1("energy1"), //
	ENERGY2("energy2"), //
	ENERGY_OUT("energyOut"), //
	ENERGY_OUT1("energyOut1"), //
	ENERGY_OUT2("energyOut2");

	private final String name;

	private Field(String name) {
		this.name = name;
	}

	/**
	 * Shortcut to get name.
	 * 
	 * @return the name
	 */
	public String n() {
		return this.name;
	}

}
