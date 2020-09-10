package io.openems.edge.battery.bmw.utils;

public class Constants {
	
	/**
	 * Retry set-command after x Seconds, e.g. for PreContactorControl.
	 */
	public static int RETRY_COMMAND_SECONDS = 30;
	
	/**
	 * Retry x attempts for set-command.
	 */
	public static int RETRY_COMMAND_MAX_ATTEMPTS = 30;
	
	public static final Integer OPEN_CONTACTORS = 0;
	public static final Integer CLOSE_CONTACTORS = 4;

	public static final Long ERROR_DELAY = 600l;
}
