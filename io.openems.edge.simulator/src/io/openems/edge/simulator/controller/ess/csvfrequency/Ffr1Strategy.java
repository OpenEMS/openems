package io.openems.edge.simulator.controller.ess.csvfrequency;

/**
 * Fast Frequency Response 1 (FFR1) Strategy.
 *
 * <p>
 * Provides a fast power response when frequency drops below a threshold.
 * Activates on under-frequency events and provides support for a configured
 * duration, followed by a recovery period with zero output.
 *
 * <ul>
 * <li>f &gt; threshold -> no response (safe area)
 * <li>f &le; threshold -> activate event, provide proportional power for
 * support_duration
 * <li>After support_duration -> zero output during recovery_period
 * </ul>
 */
public class Ffr1Strategy {

	private final double nominalFrequency;
	private final double thresholdFrequency;
	private final double k;
	private final double supportDuration;
	private final double recoveryPeriod;

	private boolean active = false;
	private double elapsedTime = 0.0;

	/**
	 * Creates a new FFR1 strategy.
	 *
	 * @param nominalFrequency   the nominal grid frequency in Hz (e.g. 50.0)
	 * @param thresholdFrequency the frequency threshold below which FFR1 activates
	 *                           (e.g. 49.5)
	 * @param k                  droop coefficient (e.g. 0.05)
	 * @param supportDuration    how long FFR1 provides power in seconds (e.g. 30)
	 * @param recoveryPeriod     recovery period after event in seconds (e.g. 300)
	 */
	public Ffr1Strategy(double nominalFrequency, double thresholdFrequency, double k, double supportDuration,
			double recoveryPeriod) {
		this.nominalFrequency = nominalFrequency;
		this.thresholdFrequency = thresholdFrequency;
		this.k = k;
		this.supportDuration = supportDuration;
		this.recoveryPeriod = recoveryPeriod;
	}

	/**
	 * Calculate the FFR1 response.
	 *
	 * <p>
	 * OpenEMS sign convention: positive = discharge, negative = charge.
	 * FFR1 activates on under-frequency events and injects power (discharge)
	 * to support the grid, so the response is always >= 0.
	 *
	 * @param frequency current grid frequency in Hz
	 * @param dt        time step in seconds (typically 1.0)
	 * @return response in per-unit [0, +∞ clamped by k]; positive = discharge
	 */
	public double next(double frequency, double dt) {
		if (this.active) {
			this.elapsedTime += dt;
			this.active = this.elapsedTime < this.recoveryPeriod;
		}

		double response;

		if (frequency > this.thresholdFrequency) {
			response = 0.0; // safe area - no response needed
		} else {
			// frequency <= threshold: under-frequency event
			if (!this.active) {
				this.elapsedTime = 0.0;
				this.active = true;
			}

			if (this.elapsedTime < this.supportDuration) {
				var delta = this.nominalFrequency - frequency;
				// Positive response = discharge to support grid
				response = Math.max(0, delta / (this.nominalFrequency * this.k));
			} else {
				// During recovery period, output is zero
				response = 0.0;
			}
		}

		return response; // positive = discharge (OpenEMS convention)
	}
}
