package io.openems.common.utils;

public class DoubleUtils {

	/**
	 * Normalize a value to a range - normalize values between [100, 1000] to range
	 * [0, 1].
	 *
	 * @param value         the value to be normalized
	 * @param minValue      the minimum possible value (e.g. "200")
	 * @param maxValue      the maximum possible value (e.g. "1000")
	 * @param minNormalized the minimum normalized value (e.g. "0")
	 * @param maxNormalized the maximum normalized value (e.g. "1")
	 * @param invert        invert the normalization, i.e. 1000 is mapped to 0 and
	 *                      200 is mapped to 1
	 * @return the normalized value
	 */
	public static double normalize(double value, double minValue, double maxValue, double minNormalized,
			double maxNormalized, boolean invert) {
		double result;
		if (value < minValue) {
			result = minNormalized;
		} else if (value > maxValue) {
			result = maxNormalized;
		} else {
			result = minNormalized + (maxNormalized - minNormalized) * ((value - minValue) / (maxValue - minValue));
		}
		if (invert) {
			return maxNormalized + minNormalized - result;
		}
		return result;
	}

}
