package io.openems.edge.battery.bydcommercial.utils;

public class Constants {

	/**
	 * Retry set-command after x Seconds, e.g. for PreContactorControl.
	 */
	public static final int RETRY_COMMAND_SECONDS = 30;

	/**
	 * Retry x attempts for set-command.
	 */
	public static final int RETRY_COMMAND_MAX_ATTEMPTS = 30;

	public static final int BAUDRATE = 57600;
	public static final int UNIT_ID = 1;
	public static final int SLAVE_UNITS = 19;
	public static final int ADDRESS_OFFSET = 0x2000;

	public static final int VOLTAGE_ADDRESS_OFFSET = 0x800;
	public static final int VOLTAGE_SENSORS_PER_MODULE = 12;

	public static final int TEMPERATURE_ADDRESS_OFFSET = 0xC00;
	public static final int TEMPERATURE_SENSORS_PER_MODULE = 12;

}
