package io.openems.edge.evcs.alpitronic;

import io.openems.common.types.SemanticVersion;

/**
 * Utility class for Alpitronic Hypercharger firmware version checks and
 * capabilities.
 */
public final class AlpitronicVersionUtils {

	private AlpitronicVersionUtils() {
		// Utility class - prevent instantiation
	}

	/**
	 * Check if this is version 1.8.x.
	 *
	 * @param version the firmware version
	 * @return true if this is version 1.8.x
	 */
	public static boolean isVersion18(SemanticVersion version) {
		return version.major() == 1 && version.minor() == 8;
	}

	/**
	 * Check if this is version 2.3.x.
	 *
	 * @param version the firmware version
	 * @return true if this is version 2.3.x
	 */
	public static boolean isVersion23(SemanticVersion version) {
		return version.major() == 2 && version.minor() == 3;
	}

	/**
	 * Check if this is version 2.4.x.
	 *
	 * @param version the firmware version
	 * @return true if this is version 2.4.x
	 */
	public static boolean isVersion24(SemanticVersion version) {
		return version.major() == 2 && version.minor() == 4;
	}

	/**
	 * Check if this is version 2.5.x or later.
	 *
	 * @param version the firmware version
	 * @return true if this is version 2.5.x or later
	 */
	public static boolean isVersion25OrLater(SemanticVersion version) {
		return version.isAtLeast(new SemanticVersion(2, 5, 0));
	}

	/**
	 * Check if version supports extended connector types (MCS, NACS).
	 *
	 * @param version the firmware version
	 * @return true if version supports extended connector types
	 */
	public static boolean supportsExtendedConnectorTypes(SemanticVersion version) {
		return isVersion25OrLater(version);
	}

	/**
	 * Check if version supports total charged energy register (132).
	 *
	 * @param version the firmware version
	 * @return true if version supports total charged energy register
	 */
	public static boolean supportsTotalChargedEnergy(SemanticVersion version) {
		return version.isAtLeast(new SemanticVersion(2, 3, 0));
	}

	/**
	 * Check if version supports Max Charging Power AC register (136).
	 *
	 * @param version the firmware version
	 * @return true if version supports Max Charging Power AC register
	 */
	public static boolean supportsMaxChargingPowerAC(SemanticVersion version) {
		return version.isAtLeast(new SemanticVersion(2, 4, 0));
	}

	/**
	 * Check if holding register 0 represents Apparent Power (VA) instead of Active
	 * Power (W).
	 *
	 * @param version the firmware version
	 * @return true if holding register 0 represents Apparent Power
	 */
	public static boolean usesApparentPowerForStation(SemanticVersion version) {
		return isVersion25OrLater(version);
	}
}