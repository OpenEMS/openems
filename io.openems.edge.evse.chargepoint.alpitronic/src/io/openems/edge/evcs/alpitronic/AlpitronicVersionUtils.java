package io.openems.edge.evcs.alpitronic;

import io.openems.common.types.SemanticVersion;

/**
 * Utility class for handling Alpitronic firmware version detection and feature
 * support.
 */
public class AlpitronicVersionUtils {

	private AlpitronicVersionUtils() {

	}

	/**
	 * Checks if the firmware version is v1.8.x.
	 *
	 * @param version the firmware version
	 * @return true if version is v1.8.x
	 */
	public static boolean isVersion18(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() == 1 && version.minor() == 8;
	}

	/**
	 * Checks if the firmware version is v2.3.x.
	 *
	 * @param version the firmware version
	 * @return true if version is v2.3.x
	 */
	public static boolean isVersion23(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() == 2 && version.minor() == 3;
	}

	/**
	 * Checks if the firmware version is v2.4.x.
	 *
	 * @param version the firmware version
	 * @return true if version is v2.4.x
	 */
	public static boolean isVersion24(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() == 2 && version.minor() == 4;
	}

	/**
	 * Checks if the firmware version is v2.5 or later.
	 *
	 * @param version the firmware version
	 * @return true if version is v2.5 or later
	 */
	public static boolean isVersion25OrLater(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() > 2 || (version.major() == 2 && version.minor() >= 5);
	}

	/**
	 * Checks if the firmware supports total charged energy (v2.3+).
	 *
	 * @param version the firmware version
	 * @return true if total charged energy is supported
	 */
	public static boolean supportsTotalChargedEnergy(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() > 2 || (version.major() == 2 && version.minor() >= 3);
	}

	/**
	 * Checks if the firmware supports maximum AC charging power (v2.4+).
	 *
	 * @param version the firmware version
	 * @return true if max AC charging power is supported
	 */
	public static boolean supportsMaxChargingPowerAC(SemanticVersion version) {
		if (version == null) {
			return false;
		}
		return version.major() > 2 || (version.major() == 2 && version.minor() >= 4);
	}

	/**
	 * Checks if the firmware uses apparent power for station (v2.5+).
	 *
	 * @param version the firmware version
	 * @return true if apparent power is used
	 */
	public static boolean usesApparentPowerForStation(SemanticVersion version) {
		return isVersion25OrLater(version);
	}

	/**
	 * Checks if the firmware supports extended connector types (v2.5+), including
	 * MCS and NACS.
	 *
	 * @param version the firmware version
	 * @return true if extended connector types are supported
	 */
	public static boolean supportsExtendedConnectorTypes(SemanticVersion version) {
		return isVersion25OrLater(version);
	}
}
