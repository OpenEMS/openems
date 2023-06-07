package io.openems.edge.battery.soltaro.versionc.utils;

public class Constants {

	/**
	 * Retry set-command after x Seconds, e.g. for PreContactorControl.
	 */
	public static final int RETRY_COMMAND_SECONDS = 30;

	/**
	 * Retry x attempts for set-command.
	 */
	public static final int RETRY_COMMAND_MAX_ATTEMPTS = 30;

	public static final int SINGLE_RACK_ADDRESS_OFFSET = 0x2000;

	public static final int VOLTAGE_ADDRESS_OFFSET = 0x800;
	public static final int VOLTAGE_SENSORS_PER_MODULE = 12;

	public static final int TEMPERATURE_ADDRESS_OFFSET = 0xC00;
	public static final int TEMPERATURE_SENSORS_PER_MODULE = 12;

	public static final int MIN_VOLTAGE_MILLIVOLT_PER_MODULE = 34_800;
	public static final int MAX_VOLTAGE_MILLIVOLT_PER_MODULE = 42_700;

}
