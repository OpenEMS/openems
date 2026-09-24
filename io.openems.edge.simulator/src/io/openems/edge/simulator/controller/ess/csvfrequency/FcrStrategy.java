package io.openems.edge.simulator.controller.ess.csvfrequency;

/**
 * Frequency Containment Reserve (FCR) Strategy.
 *
 * <p>
 * Provides a proportional power response based on frequency deviation from
 * nominal. Includes a deadband with SOC-aware behavior: within the deadband,
 * power is only provided if it moves SOC closer to the target.
 *
 * <ul>
 * <li>f &lt; nominal -> positive response (discharge)
 * <li>f &gt; nominal -> negative response (charge)
 * </ul>
 */
public class FcrStrategy {

	private final double nominalFrequency;
	private final double maxFrequencyDeviation;
	private final double frequencyDeadband;
	private final double socTarget;

	/**
	 * Creates a new FCR strategy.
	 *
	 * @param nominalFrequency      the nominal grid frequency in Hz (e.g. 50.0)
	 * @param maxFrequencyDeviation the maximum frequency deviation in Hz that maps
	 *                              to full power (e.g. 0.2)
	 * @param frequencyDeadband     the frequency deadband in Hz around nominal
	 *                              where SOC-aware logic applies (e.g. 0.01)
	 * @param socTarget             the target SOC in percent (e.g. 50.0)
	 */
	public FcrStrategy(double nominalFrequency, double maxFrequencyDeviation, double frequencyDeadband,
			double socTarget) {
		this.nominalFrequency = nominalFrequency;
		this.maxFrequencyDeviation = maxFrequencyDeviation;
		this.frequencyDeadband = frequencyDeadband;
		this.socTarget = socTarget;
	}

	/**
	 * Calculate the FCR response.
	 *
	 * @param frequency current grid frequency in Hz
	 * @param soc       current state of charge in percent [0-100]
	 * @return response in per-unit [-1, 1]; positive = discharge, negative = charge
	 */
	public double next(double frequency, double soc) {
		var delta = this.nominalFrequency - frequency;
		var response = delta / this.maxFrequencyDeviation;

		// Limit to [-1, 1]
		response = Math.max(Math.min(response, 1.0), -1.0);

		// Deadband - SOC-aware degree of freedom.
		// Within the deadband, zero the response only if it would push SOC further
		// away from target:
		// - SOC already high (>= target) AND charging (response < 0): don't charge more
		// - SOC already low (<= target) AND discharging (response > 0): don't discharge more
		if (Math.abs(delta) <= this.frequencyDeadband) {
			if ((soc >= this.socTarget && response < 0) || (soc <= this.socTarget && response > 0)) {
				response = 0.0;
			}
		}

		return response;
	}
}
