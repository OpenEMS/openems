package io.openems.common.utils;

public class IntUtils {

	public enum Round {
		UP, DOWN
	}

	/**
	 * Rounds a value to a defined precision.
	 * 
	 * Example: roundToPrecision(1234, Round.UP, 100) -> 1300
	 * 
	 * @param value
	 * @param round
	 * @param precision
	 * @return
	 */
	public static int roundToPrecision(float value, Round round, int precision) {
		switch (round) {
		case DOWN:
			return (int) (Math.floor(value / precision) * precision);
		case UP:
			return (int) ((Math.floor(value / precision) + (value % precision > 0 ? 1 : 0)) * precision);
		}
		return 0;
	}

}
