package io.openems.common.utils;

public class IntUtils {

	public enum Round {
		AWAY_FROM_ZERO, TOWARDS_ZERO
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

		if (value < 0 && round == Round.AWAY_FROM_ZERO || value > 0 && round == Round.TOWARDS_ZERO) {
			return (int) Math.floor(value / precision) * precision;
		}
		return (int) Math.ceil(value / precision) * precision;
	}

}
