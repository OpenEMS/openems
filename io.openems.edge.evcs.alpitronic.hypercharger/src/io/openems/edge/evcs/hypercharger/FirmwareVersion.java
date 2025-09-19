package io.openems.edge.evcs.hypercharger;

/**
 * Represents firmware version information and capabilities for Alpitronic
 * Hypercharger.
 */
public class FirmwareVersion {

	private final int major;
	private final int minor;
	private final int patch;

	/**
	 * Constructs a new FirmwareVersion.
	 *
	 * @param major the major version number
	 * @param minor the minor version number
	 * @param patch the patch version number
	 */
	public FirmwareVersion(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	/**
	 * Check if this version is at least the specified version.
	 *
	 * @param major the major version to compare
	 * @param minor the minor version to compare
	 * @return true if this version is at least the specified version
	 */
	public boolean isAtLeast(int major, int minor) {
		if (this.major > major) {
			return true;
		}
		if (this.major == major && this.minor >= minor) {
			return true;
		}
		return false;
	}

	/**
	 * Check if this is version 1.8.x.
	 *
	 * @return true if this is version 1.8.x
	 */
	public boolean isVersion18() {
		return this.major == 1 && this.minor == 8;
	}

	/**
	 * Check if this is version 2.3.x.
	 *
	 * @return true if this is version 2.3.x
	 */
	public boolean isVersion23() {
		return this.major == 2 && this.minor == 3;
	}

	/**
	 * Check if this is version 2.4.x.
	 *
	 * @return true if this is version 2.4.x
	 */
	public boolean isVersion24() {
		return this.major == 2 && this.minor == 4;
	}

	/**
	 * Check if this is version 2.5.x or later.
	 *
	 * @return true if this is version 2.5.x or later
	 */
	public boolean isVersion25OrLater() {
		return this.isAtLeast(2, 5);
	}

	/**
	 * Check if version supports extended connector types (MCS, NACS).
	 *
	 * @return true if version supports extended connector types
	 */
	public boolean supportsExtendedConnectorTypes() {
		return this.isVersion25OrLater();
	}

	/**
	 * Check if version supports total charged energy register (132).
	 *
	 * @return true if version supports total charged energy register
	 */
	public boolean supportsTotalChargedEnergy() {
		return this.isAtLeast(2, 3);
	}

	/**
	 * Check if version supports Max Charging Power AC register (136).
	 *
	 * @return true if version supports Max Charging Power AC register
	 */
	public boolean supportsMaxChargingPowerAC() {
		return this.isAtLeast(2, 4);
	}

	/**
	 * Check if holding register 0 represents Apparent Power (VA) instead of Active
	 * Power (W).
	 *
	 * @return true if holding register 0 represents Apparent Power
	 */
	public boolean usesApparentPowerForStation() {
		return this.isVersion25OrLater();
	}

	@Override
	public String toString() {
		return this.major + "." + this.minor + "." + this.patch;
	}

	/**
	 * Gets the major version number.
	 *
	 * @return the major version number
	 */
	public int getMajor() {
		return this.major;
	}

	/**
	 * Gets the minor version number.
	 *
	 * @return the minor version number
	 */
	public int getMinor() {
		return this.minor;
	}

	/**
	 * Gets the patch version number.
	 *
	 * @return the patch version number
	 */
	public int getPatch() {
		return this.patch;
	}
}