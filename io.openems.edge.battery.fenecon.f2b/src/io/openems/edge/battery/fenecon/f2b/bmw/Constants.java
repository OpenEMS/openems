package io.openems.edge.battery.fenecon.f2b.bmw;

//TODO reorganize after battery protection update
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
	// Voltage control / limit SOC: Force charge power -> Unit: Watt
	public static final int FORCE_CHARGE_DISCHARGE_POWER = 3000;
	// Voltage control / limit SOC: Error delay of deep discharge protection ->
	// Unit: seconds
	public static final int DEEP_DISCHARGE_PROTECTION_ERROR_DELAY = 300; // 300 s = 5 min
	// Voltage control: PT1-filter time constant -> Unit: seconds -> choose zero
	// for disabling the PT1-filter
	public static final double VOLTAGE_CONTROL_FILTER_TIME_CONSTANT = 5.0;
	// Voltage control: Offset for maximum voltage -> Unit: 0.1V
	public static final int VOLTAGE_CONTROL_OFFSET = -20;
	// Voltage control: Inner resistance of the complete battery -> Unit: Ohm
	// this value should be the max. worst case value to avoid oscillation of the
	// voltage controller
	public static final double INNER_RESISTANCE = 0.2;
	// Limit SOC: Force charge power error threshold -> Unit: Watt
	public static final int FORCE_CHARGE_POWER_ERROR_THRESHOLD = 1500;
	// Parameters to be applied for voltage regulation
	public static final int ONLY_DISCHARGE_UPPER_THRESHOLD = 1005;
	public static final int ONLY_DISCHARGE_LOWER_THRESHOLD = 995;
	public static final int ONLY_CHARGE_UPPER_THRESHOLD = 10;
	public static final int ONLY_CHARGE_LOWER_THRESHOLD = 0;
	public static final int FORCE_CHARGE_UPPER_THRESHOLD = 5;
	public static final int FORCE_CHARGE_LOWER_THRESHOLD = -5;
	public static final int REQUIRED_BATTERY_POWER_FOR_HEATING = 1000;
}
