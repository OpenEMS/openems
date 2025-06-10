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
	LOW(180, 0.5f), //

	/**
	 * The state of charge will likely be at 100% before there is less production
	 * than consumption. It is still possible that the storage is not completely
	 * full and not every PV-curtail can be covered.
	 */
	MEDIUM(120, 0.75f), //

	/**
	 * The state of charge will mostly be at 100% before there is less production
	 * than consumption but if there is very often more production than the maximum
	 * sell to grid power allows - this power can be used to charge the battery,
	 * because it is not already full in most of the cases.
	 */
	HIGH(60, 0.9f);

	/**
	 * Buffer minutes for the target time in order to work correctly even in case of
	 * deviations from the forecast.
	 */
	public final int bufferMinutes;

	/**
	 * Energy buffer as a factor to be able to operate correctly even in case of
	 * deviations from the forecast.
	 * 
	 * <p>
	 * Since the prediction of available energy is not suitable for every system, we
	 * apply this logic only to low-risk configurations.
	 */
	public final float eneryBuffer;

	private DelayChargeRiskLevel(int bufferMinutes, float eneryBuffer) {
		this.bufferMinutes = bufferMinutes;
		this.eneryBuffer = eneryBuffer;
	}

	/**
	 * Get buffer minutes.
	 *
	 * @return buffer minutes
	 */
	public int getBufferMinutes() {
		return this.bufferMinutes;
	}

	/**
	 * Get the energy buffer as factor.
	 * 
	 * @return energy buffer
	 */
	public float getEneryBuffer() {
		return this.eneryBuffer;
	}
}
