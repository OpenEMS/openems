package io.openems.edge.ess.stabl;

public class EssStableConstants {

	/** Maximum apparent power supported by the system in Watts */
	public static final int MAX_APPARENT_POWER = 66_000;

	/** Maximum value for 16-bit unsigned register */
	public static final int MAX_REGISTER_VALUE = 0xFFFF;

	/** Power scaling factor for converting between W and register values */
	public static final double POWER_SCALING_FACTOR = 10.0;
	/** Power multiplier for final power calculations */
	public static final int POWER_MULTIPLIER = 10;

	// Module and string management constants
	public static final int NUMBER_OF_STRINGS = 3;
	public static final String MODULE_CHANNEL_PREFIX = "ACTIVE_POWER_M0D_SETPOINT_STRING_";
	public static final String MODULE_CHANNEL_SUFFIX = "_MODULE_";

}
