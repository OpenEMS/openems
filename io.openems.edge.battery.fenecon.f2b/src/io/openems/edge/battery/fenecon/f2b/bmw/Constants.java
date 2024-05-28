package io.openems.edge.battery.fenecon.f2b.bmw;

public class Constants {
	// Offset for cell voltages read register
	public static final int CELL_VOLTAGE_REGISTER_OFFSET = 201;
	// Offset for cell temperatures read register
	public static final int CELL_TEMPERATURE_REGISTER_OFFSET = 301;
	public static final int NUMBER_OF_VOLTAGE_CELLS = 96;
	public static final int NUMBER_OF_TEMPERATURE_CELLS = 36;
	public static final int CELL_VOLTAGE_VALUE_OFFSET = 1800;// [mV]
	public static final int CELL_TEMPERATURE_VALUE_OFFSET = 40;// [K]
	// To heat the battery, the battery temperature must be less than this value.
	public static final int HEATING_START_TEMPERATURE = 10;
	// The power value which has to set to start heating
	public static final int RELEASED_POWER = 1000;
	// The power value which has to be set to trigger battery heating
	public static final long PREDICTED_CHARGING_POWER = 3200;
	public static final double MIN_ALLOWED_SOC = 4d;
	public static final double MAX_ALLOWED_SOC = 96d;
	public static final int REQUIRED_BATTERY_POWER_FOR_HEATING = 1000;
}
