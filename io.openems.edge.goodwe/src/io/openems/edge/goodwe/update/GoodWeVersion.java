package io.openems.edge.goodwe.update;

public record GoodWeVersion(//
		Integer dspFirmwareVersion, //
		Integer dspFirmwareVersionBeta, //
		Integer armFirmwareVersion, //
		Integer armFirmwareVersionBeta //
) {

	/**
	 * Copies this version and replaces the dsp firmware version.
	 * 
	 * @param dspFirmwareVersion the new dsp firmware version
	 * @return the new {@link GoodWeVersion}
	 */
	public GoodWeVersion withDspFirmwareVersion(Integer dspFirmwareVersion) {
		return new GoodWeVersion(dspFirmwareVersion, this.dspFirmwareVersionBeta(), this.armFirmwareVersion(),
				this.armFirmwareVersionBeta());
	}

	/**
	 * Copies this version and replaces the dsp firmware beta version.
	 * 
	 * @param dspFirmwareVersionBeta the new dsp firmware beta version
	 * @return the new {@link GoodWeVersion}
	 */
	public GoodWeVersion withDspFirmwareVersionBeta(Integer dspFirmwareVersionBeta) {
		return new GoodWeVersion(this.dspFirmwareVersion(), dspFirmwareVersionBeta, this.armFirmwareVersion(),
				this.armFirmwareVersionBeta());
	}

	/**
	 * Copies this version and replaces the arm firmware version.
	 * 
	 * @param armFirmwareVersion the new arm firmware version
	 * @return the new {@link GoodWeVersion}
	 */
	public GoodWeVersion withArmFirmwareVersion(Integer armFirmwareVersion) {
		return new GoodWeVersion(this.dspFirmwareVersion(), this.dspFirmwareVersionBeta(), armFirmwareVersion,
				this.armFirmwareVersionBeta());
	}

	/**
	 * Copies this version and replaces the arm firmware beta version.
	 * 
	 * @param armFirmwareVersionBeta the new arn firmware beta version
	 * @return the new {@link GoodWeVersion}
	 */
	public GoodWeVersion withArmFirmwareVersionBeta(Integer armFirmwareVersionBeta) {
		return new GoodWeVersion(this.dspFirmwareVersion(), this.dspFirmwareVersionBeta(), this.armFirmwareVersion(),
				armFirmwareVersionBeta);
	}

	/**
	 * Checks if all version values are defined.
	 * 
	 * @return true if all values are not null; else false
	 */
	public boolean isDefined() {
		return this.dspFirmwareVersion() != null && this.dspFirmwareVersionBeta() != null
				&& this.armFirmwareVersion() != null && this.armFirmwareVersionBeta() != null;
	}

	@Override
	public String toString() {
		return String.format("dsp %d.%d arm %d.%d", this.dspFirmwareVersion(), this.dspFirmwareVersionBeta(),
				this.armFirmwareVersion(), this.armFirmwareVersionBeta());
	}
}