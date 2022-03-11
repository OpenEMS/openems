package io.openems.edge.controller.ess.gridoptimizedcharge;

/**
 * The Risk Level is describing the risk propensity and effects on the SoC curve
 * during the day.
 */
public enum DelayChargeRiskLevel {

	/**
	 * The state of charge will most likely be at 100% before there is less
	 * production than consumption but the storage is maybe already full if we need
	 * to avoid the PV-curtail.
	 */
	LOW(180), //

	/**
	 * The state of charge will likely be at 100% before there is less production
	 * than consumption. It is still possible that the storage is not completely
	 * full and not every PV-curtail can be covered.
	 */
	MEDIUM(120), //

	/**
	 * The state of charge will mostly be at 100% before there is less production
	 * than consumption but if there is very often more production than the maximum
	 * sell to grid power allows - this power can be used to charge the battery,
	 * because it is not already full in most of the cases.
	 */
	HIGH(60);

	public final int bufferMinutes;

	private DelayChargeRiskLevel(int bufferMinutes) {
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
