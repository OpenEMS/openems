package io.openems.common.utils;

import java.util.List;

public class IntUtils {

	public enum Round {
		AWAY_FROM_ZERO, TOWARDS_ZERO, HALF_UP
	}

	/**
	 * Rounds a value to a defined precision.
	 *
	 * <p>
	 * Example: roundToPrecision(1234, Round.AWAY_FROM_ZERO, 100) -&gt; 1300
	 *
	 * @param value     the value
	 * @param round     the rounding mode
	 * @param precision the decimal precision
	 * @return the rounded value
	 */
	public static int roundToPrecision(double value, Round round, int precision) {
		if ((value == 0) || (value % precision == 0)) {
			return (int) value;
		}

		if (round.equals(Round.HALF_UP)) {
			return (int) Math.round(value / precision) * precision;
		}

		if (value < 0 && round == Round.AWAY_FROM_ZERO || value > 0 && round == Round.TOWARDS_ZERO) {
			return (int) Math.floor(value / precision) * precision;
		}
		return (int) Math.ceil(value / precision) * precision;
	}

	/**
	 * Safely finds the min value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the min value; or null if all values are null
	 */
	public static Integer minInteger(Integer... values) {
		Integer result = null;
		for (var value : values) {
			if (result != null && value != null) {
				result = Math.min(result, value);
			} else if (value != null) {
				result = value;
			}
		}
		return result;
	}

	/**
	 * Safely finds the int min value of all values.
	 *
	 * @param firstValue      the primitive first int value
	 * @param remainingValues the remaining {@link Integer} values
	 * @return the min value
	 */
	public static int minInt(int firstValue, Integer... remainingValues) {
		var remainingMin = minInteger(remainingValues);
		if (remainingMin == null) {
			return firstValue;
		} else {
			return Math.min(firstValue, remainingMin);
		}
	}

	/**
	 * Safely finds the max value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the max value; or null if all values are null
	 */
	public static Integer maxInteger(Integer... values) {
		Integer result = null;
		for (var value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.max(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the int max value of all values.
	 *
	 * @param firstValue      the primitive first int value
	 * @param remainingValues the remaining {@link Integer} values
	 * @return the max value
	 */
	public static int maxInt(int firstValue, Integer... remainingValues) {
		var remainingMin = maxInteger(remainingValues);
		if (remainingMin == null) {
			return firstValue;
		} else {
			return Math.max(firstValue, remainingMin);
		}
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Integer} values
	 * @return the sum; or null
	 */
	public static Integer sumInteger(List<Integer> values) {
		return sumInteger(values.toArray(Integer[]::new));
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Integer} values
	 * @return the sum; or null
	 */
	public static Integer sumInteger(Integer... values) {
		Integer result = null;
		for (var value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'.
	 *
	 * @param firstValue      the primitive first int value
	 * @param remainingValues the remaining {@link Integer} values
	 * @return the sum
	 */
	public static int sumInt(int firstValue, Integer... remainingValues) {
		var remainingSum = sumInteger(remainingValues);
		if (remainingSum == null) {
			return firstValue;
		} else {
			return firstValue + remainingSum;
		}
	}
}
