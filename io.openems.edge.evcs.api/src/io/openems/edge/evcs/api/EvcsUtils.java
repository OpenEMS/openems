package io.openems.edge.evcs.api;

public class EvcsUtils {

	/**
	 * Converts a given power to current in Milliampere (assuming the voltage is 230
	 * V).
	 * 
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Milliampere
	 */
	public static int powerToCurrentInMilliampere(int power, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		return Long.valueOf(Math.round(power * 1000 / Evcs.DEFAULT_VOLTAGE / phases)).intValue();
	}

	/**
	 * Converts a given power to current in Ampere (assuming the voltage is 230 V).
	 * 
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Ampere
	 */
	public static int powerToCurrent(int power, int phases) {
		return powerToCurrentInMilliampere(power, phases) / 1000;
	}

	/**
	 * Converts a given current in Ampere to power (assuming the voltage is 230V).
	 * 
	 * @param current in Ampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int currentToPower(int current, int phases) {
		return currentInMilliampereToPower(current * 1000, phases);
	}

	/**
	 * Converts a given current in Milliampere to power (assuming the voltage is
	 * 230V).
	 * 
	 * @param current in Milliampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int currentInMilliampereToPower(int current, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		var power = current * phases * Evcs.DEFAULT_VOLTAGE;
		return power / 1000;

	}

}
