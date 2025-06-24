package io.openems.edge.evcs.api;

/**
 * Provides EVCS electricity unit conversion methods.
 */
public class EvcsUtils {

	/**
	 * Converts a given power to current in Milliampere.
	 *
	 * @param power   in Watt
	 * @param voltage in Volt
	 * @param phases  active phases
	 * @return current in Milliampere
	 */
	public static int wattToMilliampere(int power, int voltage, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		return Math.round((float) (power * 1000) / voltage / phases);
	}

	/**
	 * Converts a given power to current in Milliampere (assuming the voltage is 230
	 * V).
	 *
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Milliampere
	 */
	public static int wattToMilliampere(int power, int phases) {
		return wattToMilliampere(power, Evcs.DEFAULT_VOLTAGE, phases);
	}

	/**
	 * Converts a given power to current in Ampere.
	 *
	 * @param power   in Watt
	 * @param voltage in Volt
	 * @param phases  active phases
	 * @return current in Ampere
	 */
	public static int wattToAmpere(int power, int voltage, int phases) {
		return wattToMilliampere(power, voltage, phases) / 1000;
	}

	/**
	 * Converts a given power to current in Ampere (assuming the voltage is 230 V).
	 * 
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Ampere
	 */
	public static int wattToAmpere(int power, int phases) {
		return wattToAmpere(power, Evcs.DEFAULT_VOLTAGE, phases);
	}

	/**
	 * Converts a given current in Milliampere to power in Watt.
	 *
	 * @param current in Milliampere
	 * @param voltage in Volt
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int milliampereToWatt(int current, int voltage, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		var power = current * phases * voltage;
		return power / 1000;
	}

	/**
	 * Converts a given current in Milliampere to power in Watt (assuming the
	 * voltage is 230 V).
	 *
	 * @param current in Milliampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int milliampereToWatt(int current, int phases) {
		return milliampereToWatt(current, Evcs.DEFAULT_VOLTAGE, phases);
	}

	/**
	 * Converts a given current in Ampere to power.
	 *
	 * @param current in Ampere
	 * @param voltage in Volt
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int ampereToWatt(int current, int voltage, int phases) {
		return milliampereToWatt(current * 1000, voltage, phases);
	}

	/**
	 * Converts a given current in Ampere to power in Watt (assuming the voltage is
	 * 230 V).
	 * 
	 * @param current in Ampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int ampereToWatt(int current, int phases) {
		return ampereToWatt(current, Evcs.DEFAULT_VOLTAGE, phases);
	}

}