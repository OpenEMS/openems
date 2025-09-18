package io.openems.edge.evcs.hypercharger;

/**
 * Represents firmware version information and capabilities for Alpitronic Hypercharger.
 */
public class FirmwareVersion {
	
	private final int major;
	private final int minor;
	private final int patch;
	
	public FirmwareVersion(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}
	
	/**
	 * Check if this version is at least the specified version.
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
	 * Check if this is version 1.8.x
	 */
	public boolean isVersion18() {
		return major == 1 && minor == 8;
	}
	
	/**
	 * Check if this is version 2.3.x
	 */
	public boolean isVersion23() {
		return major == 2 && minor == 3;
	}
	
	/**
	 * Check if this is version 2.4.x
	 */
	public boolean isVersion24() {
		return major == 2 && minor == 4;
	}
	
	/**
	 * Check if this is version 2.5.x or later
	 */
	public boolean isVersion25OrLater() {
		return isAtLeast(2, 5);
	}
	
	/**
	 * Check if version supports extended connector types (MCS, NACS).
	 */
	public boolean supportsExtendedConnectorTypes() {
		return isVersion25OrLater();
	}
	
	/**
	 * Check if version supports total charged energy register (132).
	 */
	public boolean supportsTotalChargedEnergy() {
		return isAtLeast(2, 3);
	}
	
	/**
	 * Check if version supports Max Charging Power AC register (136).
	 */
	public boolean supportsMaxChargingPowerAC() {
		return isAtLeast(2, 4);
	}
	
	/**
	 * Check if holding register 0 represents Apparent Power (VA) instead of Active Power (W).
	 */
	public boolean usesApparentPowerForStation() {
		return isVersion25OrLater();
	}
	
	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}
	
	public int getMajor() {
		return major;
	}
	
	public int getMinor() {
		return minor;
	}
	
	public int getPatch() {
		return patch;
	}
}