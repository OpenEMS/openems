package io.openems.edge.battery.fenecon.home;

public enum ModuleParameters {
	MODULE_MAX_VOLTAGE(50), //
	MODULE_MIN_VOLTAGE(40), //
	MIN_CELL_VOLTAGE_MILLIVOLT(10127), //
	MAX_CELL_VOLTAGE_MILLIVOLT(10128), //
	MAX_CELL_TEMPERATURE(10129), //
	MIN_CELL_TEMPERATURE(10130), //

	VOLTAGE_SENSORS_PER_MODULE(14), //
	TEMPERATURE_SENSORS_PER_MODULE(14), //
	ADDRESS_OFFSET(100), //
	VOLTAGE_ADDRESS_OFFSET(2), //
	TEMPERATURE_ADDRESS_OFFSET(18), //

	TOWER_1_OFFSET(10000), //
	TOWER_2_OFFSET(12000), //
	TOWER_3_OFFSET(14000), //

	TOWER_UNDEFINED(0);
	;

	private ModuleParameters(int value) {
		this.value = value;
	}

	private int value;

	public int getValue() {
		return this.value;
	}

}
