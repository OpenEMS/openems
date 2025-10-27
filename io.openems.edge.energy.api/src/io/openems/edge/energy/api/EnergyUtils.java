package io.openems.edge.energy.api;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

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
	 * Finds the last index of the first valley in an array of doubles, e.g. prices.
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
	 * Finds indexes of valleys in an array of doubles, e.g. prices.
	 * 
	 * @param values the values array
	 * @return a list of valleys
	 */
	public static int[] findValleyIndexes(double[] values) {
		final var result = ImmutableSet.<Integer>builder();
		int valley = 0;
		int peak = 0;
		while (true) {
			valley = findFirstValleyIndex(peak, values);
			peak = findFirstPeakIndex(valley, values);
			if (peak == valley) {
				break;
			}
			result.add(valley);
		}
		return result.build().stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * From a list of {@link EnergyScheduleHandler}s, filters only the ones of type
	 * {@link EnergyScheduleHandler.WithDifferentModes}.
	 * 
	 * @param eshs list of {@link EnergyScheduleHandler}s
	 * @return new stream of {@link EnergyScheduleHandler.WithDifferentModes}s
	 */
	public static Stream<EnergyScheduleHandler.WithDifferentModes> filterEshsWithDifferentModes(
			ImmutableList<EnergyScheduleHandler> eshs) {
		return eshs.stream() //
				.filter(EnergyScheduleHandler.WithDifferentModes.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentModes.class::cast);
		// TODO only ESHs with actually more than one Mode
	}
}
