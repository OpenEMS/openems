package io.openems.edge.goodwe.stsbox.update;

public record GoodWeStsBoxVersion(//
		Integer firmwareVersion, //
		Integer firmwareSubVersion //
) {

	/**
	 * Copies this version and replaces the dsp firmware version.
	 * 
	 * @param firmwareVersion the new dsp firmware version
	 * @return the new {@link GoodWeStsBoxVersion}
	 */
	public GoodWeStsBoxVersion withFirmwareVersion(Integer firmwareVersion) {
		return new GoodWeStsBoxVersion(firmwareVersion, this.firmwareSubVersion());
	}

	/**
	 * Copies this version and replaces the dsp firmware beta version.
	 * 
	 * @param firmwareSubVersion the new dsp firmware beta version
	 * @return the new {@link GoodWeStsBoxVersion}
	 */
	public GoodWeStsBoxVersion withFirmwareSubVersion(Integer firmwareSubVersion) {
		return new GoodWeStsBoxVersion(this.firmwareVersion(), firmwareSubVersion);
	}

	/**
	 * Checks if all version values are defined.
	 * 
	 * @return true if all values are not null; else false
	 */
	public boolean isDefined() {
		return this.firmwareVersion() != null && this.firmwareSubVersion() != null;
	}

	@Override
	public String toString() {
		return String.format("%d.%d", this.firmwareVersion(), this.firmwareSubVersion());
	}
}