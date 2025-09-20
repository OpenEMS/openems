package io.openems.common.types;

public record SemanticVersion(int major, int minor, int patch, String additional) {

	/**
	 * Creates an instance with major, minor and patch version set to zero and no
	 * additional string.
	 */
	public static final SemanticVersion ZERO = new SemanticVersion(0, 0, 0);

	/**
	 * Creates an instance using a Version-String in the form
	 * MAJOR.MINOR.PATCH-ADDITIONAL, like "2019.2.1-SNAPSHOT".
	 *
	 * @param versionString the Version-String
	 * @return the SemanticVersion instance
	 * @throws NumberFormatException on parse error
	 */
	public static SemanticVersion fromString(String versionString) throws NumberFormatException {
		short major = 0;
		short minor = 0;
		short patch = 0;
		var version1 = versionString.split("\\.", 3);
		var additional = "";
		if (version1.length > 0 && !version1[0].isEmpty()) {
			major = Short.parseShort(version1[0]);
		}
		if (version1.length > 1) {
			minor = Short.parseShort(version1[1]);
		}
		if (version1.length > 2) {
			var version2 = version1[2].split("-", 2);
			if (version2.length < 2) {
				patch = Short.parseShort(version1[2]);
			} else {
				patch = Short.parseShort(version2[0]);
				additional = version2[1];
			}
		}
		return new SemanticVersion(major, minor, patch, additional);
	}

	/**
	 * Creates an instance using a Version-String in the form
	 * MAJOR.MINOR.PATCH-ADDITIONAL, like "2019.2.1-SNAPSHOT". On Error a ZERO
	 * version is returned - avoiding an exception.
	 *
	 * @param versionString the Version-String
	 * @return the SemanticVersion instance
	 */
	public static SemanticVersion fromStringOrZero(String versionString) {
		if (versionString == null) {
			return SemanticVersion.ZERO;
		}
		try {
			return SemanticVersion.fromString(versionString);
		} catch (Exception e) {
			return SemanticVersion.ZERO;
		}
	}

	/**
	 * Creates an instance with major, minor and patch version and no additional
	 * string.
	 */
	public SemanticVersion(int major, int minor, int patch) {
		this(major, minor, patch, "");
	}

	/**
	 * Is this version at least as high as the given {@link SemanticVersion}?.
	 *
	 * @param o the given version
	 * @return true if this version is greater or equal to the given version
	 */
	public boolean isAtLeast(SemanticVersion o) {
		if (this.major > o.major) {
			return true;
		}
		if (this.major < o.major) {
			return false;
		}
		// major is equal
		if (this.minor > o.minor) {
			return true;
		}
		if (this.minor < o.minor) {
			return false;
		}
		// major and minor are equal
		if (this.patch > o.patch) {
			return true;
		}
		if (this.patch < o.patch) {
			return false;
		}
		// major, minor and patch are equal
		if (this.additional.isEmpty() && !o.additional.isEmpty()) {
			return true;
		}
		return this.additional.compareTo(o.additional) >= 0;
	}

	@Override
	public String toString() {
		var b = new StringBuilder() //
				.append(this.major) //
				.append(".") //
				.append(this.minor) //
				.append(".") //
				.append(this.patch);
		if (!this.additional.isEmpty()) {
			b.append("-").append(this.additional);
		}
		return b.toString();
	}
}
