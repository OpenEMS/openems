package io.openems.edge.energy.api;

import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.energy.api.EnergyConstants.PERIODS_PER_HOUR;
import static java.util.Arrays.stream;

import java.util.Objects;
import java.util.stream.IntStream;

public class EnergyUtils {

	private EnergyUtils() {
	}

	/**
	 * Converts a State-of-Charge [%] to Energy [Wh].
	 * 
	 * @param totalEnergy the total energy in [Wh]
	 * @param soc         the State-of-Charge in [%]
	 * @return the energy in [Wh]
	 */
	public static int socToEnergy(int totalEnergy, int soc) {
		return totalEnergy /* [Wh] */ / 100 * soc;
	}

	/**
	 * Finds the first valley in an array of doubles, e.g. prices.
	 * 
	 * @param fromIndex start searching from this index
	 * @param values    the values array
	 * @return the index of the valley
	 */
	public static int findFirstValleyIndex(int fromIndex, double[] values) {
		if (values.length <= fromIndex) {
			return fromIndex;
		} else {
			var previous = values[fromIndex];
			for (var i = fromIndex + 1; i < values.length; i++) {
				var value = values[i];
				if (value > previous) {
					return i - 1;
				}
				previous = value;
			}
		}
		return values.length - 1;
	}

	/**
	 * Finds the first peak in an array of doubles, e.g. prices.
	 * 
	 * @param fromIndex start searching from this index
	 * @param values    the values array
	 * @return the index of the peak
	 */
	public static int findFirstPeakIndex(int fromIndex, double[] values) {
		if (values.length <= fromIndex) {
			return fromIndex;
		} else {
			var previous = values[fromIndex];
			for (var i = fromIndex + 1; i < values.length; i++) {
				var value = values[i];
				if (value < previous) {
					return i - 1;
				}
				previous = value;
			}
		}
		return values.length - 1;
	}

	/**
	 * Converts power [W] to energy [Wh/15 min].
	 * 
	 * @param power the power value
	 * @return the energy value
	 */
	public static int toEnergy(int power) {
		return power / PERIODS_PER_HOUR;
	}

	/**
	 * Converts energy [Wh/15 min] to power [W].
	 * 
	 * @param energy the energy value
	 * @return the power value
	 */
	public static Integer toPower(Integer energy) {
		return multiply(energy, PERIODS_PER_HOUR);
	}

	/**
	 * Interpolate an Array of {@link Integer}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	public static int[] interpolateArray(Integer[] values) {
		var firstNonNull = stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var lastNonNullIndex = IntStream.range(0, values.length) //
				.filter(i -> values[i] != null) //
				.reduce((first, second) -> second); //
		if (lastNonNullIndex.isEmpty()) {
			return new int[0];
		}
		var result = new int[lastNonNullIndex.getAsInt() + 1];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		int last = firstNonNull.get();
		for (var i = 0; i < result.length; i++) {
			int value = orElse(values[i], last);
			result[i] = last = value;
		}
		return result;
	}
}
