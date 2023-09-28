package io.openems.edge.battery.enfasbms.utils;

public class Constants {

	/**
	 * Maximum allowed time to start battery.
	 */
	public static final int MAX_ALLOWED_START_TIME_SECONDS = 30;

	/**
	 * Maximum allowed time to stop battery.
	 */
	public static final int MAX_ALLOWED_STOP_TIME_SECONDS = 30;

	/**
	 * Maximum allowed time in go stopped state.
	 */
	public static final int MAX_ALLOWED_GO_STOP_TIME_SECONDS = 120;

	/**
	 * Time for reduce Power in Error Case.
	 */
	public static final int ERROR_HANDLER_CONTACTOR_OPEN_DELAY_SECONDS = 5;

	/**
	 * max 30 Seconds in UNDEFINED.
	 */
	public static final int MAX_UNDEFINED_TIME_SECONDS = 30;

	/**
	 * Maximum allowed time in go stopped state.
	 */
	public static final int MAX_ALLOWED_TIME_SECONDS_IN_ERROR_BEFORE_OPENING_CONTACTORS = 30;

}