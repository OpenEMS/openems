package io.openems.edge.controller.ess.timeofusetariff.discharge;

/**
 * The Risk Level is describing the risk propensity and effects on the SoC curve
 * during the night.
 */
public enum DelayDischargeRiskLevel {

	/**
	 * Less dependent on predictions. The state of charge will most likely be at
	 * minimum SoC level before there is more production than consumption, but might
	 * end up buying from grid during high price hour for consumption.
	 */
	LOW(60), //

	/**
	 * Moderately dependent on predictions. The state of charge will likely be at
	 * minimum SoC level before there is more production than consumption. It is
	 * still possible that the storage might be empty and end up buying from grid
	 * during the high price hour.
	 */
	MEDIUM(30), //

	/**
	 * Complete dependency on Predictions. The state of charge will likely be at
	 * minimum SoC level before there is more production than consumption, but very
	 * often certain percentage SoC will remain in the battery which goes unused for
	 * the night consumption.
	 */
	HIGH(0);

	public final int bufferMinutes;

	private DelayDischargeRiskLevel(int bufferMinutes) {
		this.bufferMinutes = bufferMinutes;
	}

	/**
	 * Get buffer minutes.
	 *
	 * @return buffer minutes
	 */
	public int getBufferMinutes() {
		return this.bufferMinutes;
	}
}
