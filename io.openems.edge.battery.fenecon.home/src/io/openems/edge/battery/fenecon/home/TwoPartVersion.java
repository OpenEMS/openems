package io.openems.edge.battery.fenecon.home;

/**
 * Represents a two-part version number in the form MAJOR.MINOR.
 * 
 * <p>
 * Example: 1.2.
 * 
 * SemanticVersion is not used here, because Fenecon Home uses a two part
 * versioning.
 * </p>
 */
public record TwoPartVersion(int major, int minor) {

	/**
	 * Creates a TwoPartVersion with both major and minor set to zero.
	 */
	public static final TwoPartVersion ZERO = new TwoPartVersion(0, 0);

	/**
	 * Creates an instance from a version string in the form MAJOR.MINOR.
	 *
	 * @param versionString the version string, e.g. "1.2"
	 * @return the TwoPartVersion instance
	 * @throws NumberFormatException on parse error
	 */
	public static TwoPartVersion fromString(String versionString) throws NumberFormatException {
		if (versionString == null || versionString.isEmpty()) {
			return ZERO;
		}
		var parts = versionString.split("\\.", 2);
		int major = 0;
		int minor = 0;
		if (parts.length > 0 && !parts[0].isEmpty()) {
			major = Integer.parseInt(parts[0]);
		}
		if (parts.length > 1 && !parts[1].isEmpty()) {
			minor = Integer.parseInt(parts[1]);
		}
		return new TwoPartVersion(major, minor);
	}

	/**
	 * Creates an instance from a version string, returning ZERO on error.
	 *
	 * @param versionString the version string
	 * @return the TwoPartVersion instance
	 */
	public static TwoPartVersion fromStringOrZero(String versionString) {
		try {
			return fromString(versionString);
		} catch (Exception e) {
			return ZERO;
		}
	}

	/**
	 * Creates an instance from a 16-bit register value. High byte = major version,
	 * low byte = minor version.
	 *
	 * <p>
	 * Example: 0x0102 -> major=1, minor=2
	 * </p>
	 *
	 * @param registerValue 16-bit encoded version
	 * @return the TwoPartVersion instance
	 */
	public static TwoPartVersion fromRegisterValue(Integer registerValue) {
		if (registerValue == null) {
			return ZERO;
		}
		int major = (registerValue >> 8) & 0xFF;
		int minor = registerValue & 0xFF;
		return new TwoPartVersion(major, minor);
	}

	/**
	 * Checks if this version is at least as high as the given
	 * {@link TwoPartVersion}.
	 *
	 * @param o the other version
	 * @return true if this version >= other version
	 */
	public boolean isAtLeast(TwoPartVersion o) {
		if (this.major > o.major) {
			return true;
		}
		if (this.major < o.major) {
			return false;
		}
		// majors are equal
		return this.minor >= o.minor;
	}

	@Override
	public String toString() {
		return this.major + "." + this.minor;
	}
}