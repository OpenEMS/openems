package io.openems.edge.common.filter;

/**
 * A low-pass filter for smoothing values over time.
 *
 * <p>
 * This filter uses a first-order IIR (Infinite Impulse Response) filter formula
 * to smooth noisy or rapidly changing input signals. It is commonly used in
 * power control systems to gradually transition to target power levels.
 *
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/Low-pass_filter">https://en.wikipedia.org/wiki/Low-pass_filter</a>
 *
 * @see <a href= "https://github.com/OpenEMS/openems/pull/3581">Discussion on
 *      GitHub</a>
 */
public non-sealed class LowPassFilter extends Filter {

	/**
	 * Default filter coefficient. Recommended value: 0.63 (roughly 1-e^-1).
	 * 
	 * <p>
	 * Higher values (closer to 1.0) = faster response, less smoothing<br>
	 * Lower values (closer to 0.0) = slower response, more smoothing
	 */
	public static final double DEFAULT_ALPHA = 0.63;

	private final double alpha;

	private boolean firstRun = true;
	private double lastOutput = 0;

	/**
	 * Creates a LowPassFilter with the given alpha coefficient.
	 *
	 * @param alpha the filter coefficient (0.0 to 1.0)
	 *              <ul>
	 *              <li>0.0 = maximum smoothing (output follows input very slowly)
	 *              <li>0.63 = recommended default value
	 *              <li>1.0 = no smoothing (output follows input immediately)
	 *              </ul>
	 * @throws IllegalArgumentException if alpha is not between 0.0 and 1.0
	 */
	public LowPassFilter(double alpha) {
		if (alpha < 0.0 || alpha > 1.0) {
			throw new IllegalArgumentException("Alpha must be between 0.0 and 1.0, got: " + alpha);
		}
		this.alpha = alpha;
	}

	/**
	 * Apply the low-pass filter to smooth the input value.
	 *
	 * <p>
	 * Uses the first-order IIR filter formula:
	 * {@code output = alpha * input + (1 - alpha) * lastOutput}
	 *
	 * @param input the input value to be smoothed
	 * @return the filtered output value
	 */
	public int applyLowPassFilter(int input) {
		// Pre-process the input value: apply output value limits
		int limitedInput = this.applyLowHighLimits(input);

		// On first run, initialize output to input
		if (this.firstRun) {
			this.lastOutput = limitedInput;
			this.firstRun = false;
			return limitedInput;
		}

		// Apply first-order low-pass filter formula:
		// output = alpha * input + (1 - alpha) * lastOutput
		double filteredOutput = this.alpha * limitedInput + (1.0 - this.alpha) * this.lastOutput;

		// Convert to integer and apply limits
		int output = this.applyLowHighLimits(Math.round((float) filteredOutput));

		// Store last output for next iteration
		this.lastOutput = output;

		return output;
	}
}