package io.openems.edge.battery.fenecon.home;

public enum ModuleParameters {
	
	
	
	VOLTAGE_ADDRESS_OFFSET(2), //

	VOLTAGE_SENSORS_PER_MODULE(14), //
	TEMPERATURE_SENSORS_PER_MODULE(14), //
	TEMPERATURE_ADDRESS_OFFSET(18), //

	MODULE_MIN_VOLTAGE(40), //
	MODULE_MAX_VOLTAGE(50), //

	ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP(100), //

	MIN_CELL_VOLTAGE_MILLIVOLT(10127), //
	MAX_CELL_VOLTAGE_MILLIVOLT(10128), //
	MAX_CELL_TEMPERATURE(10129), //
	MIN_CELL_TEMPERATURE(10130), //

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

	public static ModuleParameters getEnum(int enumValue) {

		for (ModuleParameters i : ModuleParameters.values()) {
			if (i.getValue() == enumValue) {
				return i;
			}
		}
	     throw new IllegalArgumentException("the given number doesn't match any Status.");
	}
}
